# EC2 배포 런북

`ranking.<도메인>`(프런트) + `api.ranking.<도메인>`(백엔드)을 EC2 인스턴스 하나에서, Caddy가 HTTPS를
자동으로 처리하는 구조로 띄우는 전체 절차입니다. 세 개의 docker-compose 스택(Postgres+백엔드 /
프런트엔드 / Caddy)이 `kwtc-ranking`이라는 공유 Docker 네트워크로 서로를 이름으로 찾습니다.

## 0. 사전 준비

- 도메인 하나 (예: `kwtc.example`) — 등록만 되어 있으면 되고, DNS는 아래 3번에서 설정
- AWS 계정, EC2 인스턴스 하나 (t3.micro/t3.small 정도면 충분한 규모)
- 인스턴스에 고정 퍼블릭 IP 필요 — **Elastic IP를 할당해서 붙이세요.** 안 그러면 인스턴스를
  재시작할 때마다 IP가 바뀌어서 DNS가 깨집니다.

## 1. EC2 보안그룹

인바운드 규칙:

| 포트 | 용도 | 소스 |
|---|---|---|
| 22 | SSH | 본인 IP만 (0.0.0.0/0 금지) |
| 80 | HTTP (Let's Encrypt 인증 + HTTPS로 리다이렉트) | 0.0.0.0/0 |
| 443 | HTTPS | 0.0.0.0/0 |

**5432(Postgres), 8080(백엔드), 프런트 컨테이너 포트는 열지 마세요.** 이 구성에서는 Caddy만
외부에 노출되고, 나머지는 Docker 내부 네트워크로만 통신합니다 — 인스턴스 안에서 curl로 직접
확인하고 싶으면 SSH로 들어가서 `localhost`로 확인하면 됩니다.

## 2. 인스턴스에 Docker 설치

```
sudo apt update && sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker $USER
# 로그아웃 후 재접속 (그룹 반영)
```

## 3. DNS

도메인 등록업체(또는 Route 53)에서 A 레코드 두 개를 EC2의 **Elastic IP**로 설정:

```
ranking.kwtc.example       A   <Elastic IP>
api.ranking.kwtc.example   A   <Elastic IP>
```

전파될 때까지 몇 분~몇 시간 걸릴 수 있습니다. `dig ranking.kwtc.example`로 확인하고 넘어가세요 —
DNS가 안 잡힌 상태로 Caddy를 띄우면 Let's Encrypt 인증이 실패합니다.

## 4. 두 레포 clone

```
git clone https://github.com/KWTC-Ranking/Backend.git
git clone https://github.com/KWTC-Ranking/Frontend.git
```

## 5. 공유 네트워크 생성 (한 번만)

```
docker network create kwtc-ranking
```

## 6. 백엔드 실행

```
cd Backend
cp .env.example .env
```

`.env`를 채웁니다:
- `RANKING_JWT_SECRET` — 긴 랜덤 문자열 (`openssl rand -base64 48`)
- `RANKING_ADMIN_PASSWORD` — 첫 관리자 비밀번호
- `POSTGRES_PASSWORD` — DB 비밀번호
- `RANKING_ALLOWED_ORIGINS=https://ranking.kwtc.example`

```
docker compose -f docker-compose.yml -f docker-compose.app.yml up -d --build
```

## 7. 프런트엔드 실행

```
cd ../Frontend
cp .env.example .env
```

`.env`를 채웁니다:
- `VITE_API_BASE_URL=https://api.ranking.kwtc.example` (**https**, 실제 도메인 — 이 값은 빌드 시점에
  JS 번들에 박힙니다)

```
docker compose up -d --build
```

## 8. Caddy(리버스 프록시 + HTTPS) 실행

```
cd ../Backend/deploy
cp .env.example .env
```

`.env`를 채웁니다:
- `FRONTEND_DOMAIN=ranking.kwtc.example`
- `API_DOMAIN=api.ranking.kwtc.example`

```
docker compose up -d
docker compose logs -f caddy
```

로그에 인증서 발급 성공 메시지가 보이면 완료입니다. `https://ranking.kwtc.example`로 접속해서
로그인 → 리더보드까지 확인하세요.

## 9. 자동 백업

`deploy/backup.sh`가 `ranking-postgres` 컨테이너를 `pg_dump`로 덤프해서 `~/backups/`에
gzip으로 저장하고, 14일 지난 백업은 자동으로 지웁니다. 볼륨 안이 아니라 홈 디렉터리에 저장하므로
`docker compose down -v`를 실수로 돌려도 백업은 남습니다.

```
chmod +x ~/Backend/deploy/backup.sh
mkdir -p ~/backups
~/Backend/deploy/backup.sh   # 한 번 수동 실행해서 정상 동작 확인
ls -lh ~/backups
```

매일 새벽 4시에 자동 실행되도록 cron 등록:

```
crontab -e
```

아래 한 줄 추가:

```
0 4 * * * /home/ubuntu/Backend/deploy/backup.sh >> /home/ubuntu/backups/backup.log 2>&1
```

(`ubuntu`는 실제 로그인 사용자명으로, 경로는 `git clone`한 실제 위치로 맞추세요. `whoami`, `pwd`로 확인.)

**복구 방법** (백업 파일 하나를 통째로 복원):

```
gunzip -c ~/backups/ranking-20260101-040000.sql.gz | docker exec -i ranking-postgres psql -U ranking -d ranking
```

**더 안전하게 하려면**: `~/backups/`도 결국 같은 EC2 인스턴스 안이라, 인스턴스/EBS 자체가
통째로 날아가는 상황(드묾)까지는 못 막습니다. 가끔 한 번씩 로컬 PC로 내려받아두면 완전히
별도 위치에 사본이 생깁니다:

```
scp -i <pem 파일> ubuntu@<서버 IP>:~/backups/ranking-*.sql.gz .
```

## 재배포

**백엔드 코드 변경 시**:
```
cd Backend && git pull
docker compose -f docker-compose.yml -f docker-compose.app.yml up -d --build app
```

**프런트엔드 코드 변경 시**:
```
cd Frontend && git pull
docker compose up -d --build
```

**API 주소나 CORS 허용 도메인이 바뀌면**: 두 `.env`를 수정한 뒤 각각 위와 동일하게
`up -d --build`로 다시 빌드해야 합니다. 재시작만으로는 반영되지 않습니다 (프런트는 빌드 시점에
값이 박히고, 백엔드는 컨테이너 생성 시점에 환경변수가 고정됩니다).

## 점검 목록

- [ ] Elastic IP 할당했는지
- [ ] 보안그룹에 22/80/443만 열려있는지 (5432, 8080은 닫혀있는지)
- [ ] DNS A 레코드 두 개 다 전파됐는지 (`dig`로 확인)
- [ ] `.env` 세 개(Backend, Frontend, deploy) 전부 실제 값으로 채웠는지, 특히 관리자 비밀번호를
      기본값에서 바꿨는지
- [ ] `deploy/backup.sh`를 cron에 등록했는지 (9번 참고)
- [ ] 첫 배포 후 관리자로 로그인해서 비밀번호를 바로 바꿨는지
