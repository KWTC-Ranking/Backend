# Tennis Club Ranking Service

테니스 동아리의 단식/복식 랭킹을 관리하는 Spring Boot 백엔드입니다. 경기 결과를 기록하고, 티어 + 세트
격차 가중치 공식으로 랭킹 점수를 계산하며, 종목별로 4개 티어를 자동 재배정합니다.

## 스택

- Java 21, Spring Boot 3.3, Gradle (Groovy DSL)
- Spring Web, Spring Data JPA, Bean Validation, Spring Security (JWT)
- PostgreSQL + Flyway 마이그레이션
- springdoc-openapi (Swagger UI)
- JUnit 5, AssertJ, Mockito, Testcontainers

docker-compose 파일은 의도적으로 두 개로 나눠져 있습니다: `docker-compose.yml`(PostgreSQL만, 별도
설정 없이 바로 사용 가능)과 `docker-compose.app.yml`(앱 컨테이너를 추가하는 오버레이 — 시크릿 필요).
Compose는 실행하지 않는 서비스라도 불러온 파일에 있는 서비스라면 환경변수 플레이스홀더를 전부
검증하기 때문에, 앱을 두 번째 파일로 분리해둬야 `docker compose up -d` 하나만으로 로컬 Postgres를
아무 설정 없이 띄울 수 있습니다.

## 로컬 실행 (개발용 — 앱은 호스트에서, DB는 Docker에서)

1. PostgreSQL 실행 (`.env` 불필요 — db/user/password 기본값은 전부 `ranking`):
   ```
   docker compose up -d
   ```
2. 앱 실행 (시작할 때 Flyway가 스키마를 마이그레이션합니다):
   ```
   ./gradlew bootRun
   ```
3. Swagger UI: http://localhost:8080/swagger-ui.html

## 배포 (docker-compose, DB + 앱 함께)

Postgres와 패키징된 Spring Boot 앱을 한 호스트에서 컨테이너로 함께 띄우는 방식입니다. 포함된
`Dockerfile`(멀티스테이지: Gradle 빌드 → 슬림 JRE 런타임 이미지)로 빌드합니다.

1. 환경변수 템플릿을 복사하고 실제 시크릿 값을 채웁니다:
   ```
   cp .env.example .env
   ```
   최소한 `RANKING_JWT_SECRET`과 `RANKING_ADMIN_PASSWORD`는 반드시 설정해야 합니다 — 값이 없으면
   compose가 `app` 서비스 시작을 아예 거부합니다(안전하지 않게 조용히 실행되는 대신 명확한 오류로
   즉시 실패). PowerShell로 시크릿 생성:
   ```
   [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
   ```
2. 빌드 후 전체 실행 (`-f` 플래그 두 개를 써야 app 오버레이가 함께 적용됩니다):
   ```
   docker compose -f docker-compose.yml -f docker-compose.app.yml up -d --build
   ```
3. 정상적으로 떴는지 확인:
   ```
   docker compose -f docker-compose.yml -f docker-compose.app.yml logs -f app
   ```
   Flyway가 마이그레이션을 적용하는 로그와, (최초 1회에 한해) `AdminAccountSeeder`가 관리자 계정을
   생성했다는 로그가 보이면 정상입니다.
4. 이제 `http://<host>:${APP_PORT:-8080}`에서 API에 접근할 수 있습니다 (Swagger UI는
   `/swagger-ui.html`). 시드된 관리자 계정으로 로그인해서([인증](#인증) 참고) 비밀번호를 바꿔주세요.

**코드 변경 후 재배포**:
```
git pull && docker compose -f docker-compose.yml -f docker-compose.app.yml up -d --build app
```
앱 이미지만 다시 빌드하고 해당 컨테이너만 재생성합니다. Postgres와 그 데이터 볼륨
(`ranking-pgdata`)은 그대로 유지됩니다.

**주의할 점**: `POSTGRES_PASSWORD`는 Postgres가 빈 볼륨을 최초로 초기화할 때만 적용됩니다. (위 1번
단계 등으로) DB를 한 번이라도 띄운 적이 있는 상태에서 전체 스택을 실행하기 전에 `.env`의
`POSTGRES_PASSWORD`를 바꾸면, 기존 볼륨에는 예전 비밀번호가 그대로 남아있어서 앱이 접속에 실패합니다
(비밀번호 불일치). 해결책: 처음부터 비밀번호를 일관되게 유지하거나, 전체 스택을 처음 실행하기 전에
`docker compose down -v`로 볼륨을 한 번 지우세요.

**실제(로컬이 아닌) 배포 시 참고사항**:
- TLS를 위해 nginx/Caddy 같은 리버스 프록시 뒤에 두세요 — 앱 자체는 순수 HTTP만 서빙합니다.
- Postgres의 5432 포트를 인터넷에 노출하지 마세요. 현재 구성상 `app` 컨테이너만 compose 내부
  네트워크를 통해 접근하므로, 외부에 노출할 필요가 없습니다.
- `.env`의 `RANKING_ALLOWED_ORIGINS`를 프런트엔드가 실제로 서빙되는 주소로 설정하세요 (예:
  `https://ranking.kwtc.example`). 기본값은 로컬 Vite 개발 서버 주소라서, 이걸 안 바꾸면 배포된
  프런트엔드에서 오는 요청이 전부 브라우저 CORS 에러로 막힙니다. 여러 개면 콤마로 구분해서 나열하면
  됩니다.
- 확신이 서지 않는 업그레이드 전에는 `ranking-pgdata` 볼륨을 백업하세요 (예:
  `docker exec ranking-postgres pg_dump -U ranking ranking`).

## 테스트

```
./gradlew test
```

- `ScoringServiceTest`, `TierRecalculationServiceTest` — 순수 단위 테스트, Docker 불필요.
- `*ControllerWebMvcTest` — `@WebMvcTest` 슬라이스 테스트, Docker 불필요.
- `MatchRecordingServiceIT`, `TierRecalculationIT`, `SecurityIT` — 실제 PostgreSQL 컨테이너를 띄우는
  Testcontainers 통합 테스트. **Docker 데몬이 실행 중이어야 합니다.**

## 인증

API는 JWT Bearer 인증으로 보호됩니다. 데이터베이스에 직접 뭔가를 넣을 필요는 **없습니다** — 최초
실행 시 앱이 기본 관리자 계정을 자동으로 생성합니다 (`AdminAccountSeeder` 참고):

- 아이디: `admin` (환경변수 `RANKING_ADMIN_USERNAME`으로 재정의 가능)
- 비밀번호: `ChangeMe123!` (환경변수 `RANKING_ADMIN_PASSWORD`로 재정의 가능 — 공유/배포되는 환경
  이라면 **최초 실행 전에 반드시 설정하세요**. 이 시더는 `ADMIN` 역할 선수가 하나도 없을 때 딱
  한 번만 실행됩니다)

로그인해서 토큰 발급:

```
POST /api/auth/login
{ "username": "admin", "password": "ChangeMe123!" }

-> { "token": "...", "playerId": 1, "username": "admin", "role": "ADMIN" }
```

이후 모든 요청에 토큰을 실어 보내세요: `Authorization: Bearer <token>`.

- `POST /api/players`(동아리원 추가), `PUT /api/players/{id}`(프로필 수정),
  `PUT /api/players/{id}/password`(비밀번호 초기화), 그리고 `/api/admin/**` 전체는 `ADMIN` 역할이
  필요합니다.
- 나머지 `/api/**` 엔드포인트는 로그인만 되어 있으면 됩니다 (역할 무관).
- `POST /api/auth/login`과 Swagger UI 경로는 인증 없이 열려 있습니다.

그 외 JWT 관련 설정(`application.yml`의 `ranking.security.*`): `jwt-secret`(환경변수
`RANKING_JWT_SECRET`으로 재정의 — 실제 배포에서는 반드시 설정, 기본값은 개발용)과
`jwt-expiration-minutes`(기본값 1440 = 24시간).

### 비밀번호 변경

두 가지 방법이 있고, 둘 다 새 비밀번호를 bcrypt로 해시해서 저장합니다:

- **본인이 직접 변경** (관리자 포함, 로그인한 본인이 자기 비밀번호를 바꿈 — 현재 비밀번호 필요):
  ```
  POST /api/auth/change-password      (Authorization: Bearer <내 토큰>)
  { "currentPassword": "...", "newPassword": "8자 이상" }
  ```
  `currentPassword`가 틀리면 `401`. 최초 로그인 직후 시드된 관리자 비밀번호를 바꿀 때 사용하세요.
- **관리자가 초기화** (회원이 비밀번호를 잊어버린 경우 — 관리자가 현재 비밀번호 없이 바로 새
  비밀번호로 설정):
  ```
  PUT /api/players/{id}/password      (Authorization: Bearer <관리자 토큰>)
  { "newPassword": "8자 이상" }
  ```

### 동아리원 추가

관리자만 회원을 추가할 수 있으며, 기존에 쓰던 선수 생성 엔드포인트가 이제 로그인 정보까지 함께
받도록 확장되었습니다:

```
POST /api/players          (Authorization: Bearer <관리자 토큰>)
{
  "fullName": "홍길동",
  "email": "hong@example.com",
  "username": "hong",
  "password": "8자 이상",
  "role": "MEMBER"          // 선택, 생략 시 MEMBER — 다른 관리자를 추가할 때만 명시
}
```

새로 추가된 회원은 그 `username`/`password`로 `POST /api/auth/login`에서 바로 로그인할 수 있습니다.
자체 회원가입 엔드포인트는 의도적으로 만들지 않았습니다 — 관리자(동아리장)가 한 명씩 등록하는
구조입니다.

### Swagger UI에서 테스트하기

Swagger UI(`/swagger-ui.html`)에는 자물쇠("Authorize") 버튼이 있습니다. `OpenApiConfig`가
`bearerAuth` HTTP-bearer 보안 스킴을 선언하고 전역으로 붙여놨기 때문에, springdoc이 모든 오퍼레이션에
자물쇠 아이콘을 렌더링합니다.

1. `POST /api/auth/login`을 펼치고 → *Try it out* → 아이디/비밀번호로 실행합니다.
2. 응답 본문에서 `token` 값만 복사합니다 (JSON 전체가 아니라, `Bearer `도 붙이지 않고 값만).
3. 우측 상단 **Authorize** 버튼(또는 아무 오퍼레이션의 자물쇠 아이콘) 클릭 → `bearerAuth` 칸에 토큰
   붙여넣기 → **Authorize** → **Close**.
4. 이후 Swagger UI가 보내는 모든 요청에 자동으로 `Authorization: Bearer <token>`이 실려서, 보호된
   엔드포인트도 그 자리에서 바로 테스트할 수 있습니다. 토큰이 만료되거나(`jwt-expiration-minutes`,
   기본 24시간) 비밀번호를 바꿔서 다시 로그인해야 할 때는 1~3단계를 다시 반복하세요.

## 점수 계산 모델

경기가 기록될 때마다:

```
setMargin    = setsWonByWinner - setsWonByLoser         (예: 4-0 -> 4, 4-3 -> 1)
marginWeight = min(1 + setMargin * 0.1, ranking.margin-weight-cap)   (기본 cap 2.0)
tierWeight   = lookup(tier_weight_config, winnerTier, loserTier)     (관리자가 조정 가능, 시드된 4x4 매트릭스)
winnerPoints = round(ranking.base-points * tierWeight * marginWeight)
loserPoints  = round(winnerPoints * ranking.loser-consolation-ratio) (기본 비율 0 -> 패자는 0점)
```

- 티어(1이 최상위, 4가 최하위)는 **수동으로 지정하지 않습니다**. 매 경기 후, 같은 종목(단식/복식은
  완전히 별도로 관리)의 전체 선수를 점수순으로 재정렬해서 4분위로 다시 나눕니다.
- 새로 추가된 선수는 해당 종목에서 첫 경기를 치르기 전까지는 랭킹 row 자체가 없습니다. 첫 경기를
  치르면 0점 / 티어 4로 시작합니다.
- 복식 랭킹은 (매 경기 파트너가 바뀌므로) 고정 팀이 아니라 개인별로 집계됩니다. 복식 경기의 점수
  계산에 쓰이는 "팀 티어"는 두 파트너 티어의 평균(반올림)이며, 이긴 팀의 두 파트너는 각각
  `winnerPoints` 전액을 받습니다 (나누지 않음).
- 모든 점수 지급은 `PointTransaction` 감사 기록으로 남습니다 (기본점수/티어가중치/마진가중치, 당시
  티어, 전/후 점수) — `GET /api/players/{id}/point-history`에서 확인 가능합니다.
- 관리자는 재배포 없이 런타임에 16칸짜리 티어 가중치 매트릭스를 `GET/PUT /api/admin/tier-weights`로
  조정할 수 있습니다.

## 주요 엔드포인트

- `POST /api/auth/login`, `POST /api/auth/change-password`
- `POST /api/players`(ADMIN 전용), `GET /api/players`, `GET/PUT /api/players/{id}`(PUT은 ADMIN 전용)
- `PUT /api/players/{id}/password`(ADMIN 전용 — 회원 비밀번호 초기화)
- `GET /api/players/{id}/rankings`, `GET /api/players/{id}/point-history`
- `POST /api/matches` — 단식/복식 경기 결과 기록 (팀 구성 + 세트별 게임 스코어)
- `GET /api/matches`, `GET /api/matches/{id}`
- `GET /api/leaderboards/singles`, `GET /api/leaderboards/doubles`
- `GET/PUT /api/admin/tier-weights`

전체 요청/응답 형태는 앱 실행 후 Swagger UI에서 확인할 수 있습니다.
