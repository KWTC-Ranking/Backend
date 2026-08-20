# Tennis Club Ranking Service

Spring Boot backend for a tennis club's singles/doubles rankings. Tracks matches, computes ranking
points with a tier + margin-weighted formula, and auto-requartiles players into 4 tiers per discipline.

## Stack

- Java 21, Spring Boot 3.3, Gradle (Groovy DSL)
- Spring Web, Spring Data JPA, Bean Validation, Spring Security (JWT)
- PostgreSQL + Flyway migrations
- springdoc-openapi (Swagger UI)
- JUnit 5, AssertJ, Mockito, Testcontainers

## Running locally

1. Start PostgreSQL:
   ```
   docker compose up -d
   ```
2. Run the app (Flyway migrates the schema on startup):
   ```
   ./gradlew bootRun
   ```
3. Swagger UI: http://localhost:8080/swagger-ui.html

## Tests

```
./gradlew test
```

- `ScoringServiceTest`, `TierRecalculationServiceTest` — pure unit tests, no Docker required.
- `*ControllerWebMvcTest` — `@WebMvcTest` slices, no Docker required.
- `MatchRecordingServiceIT`, `TierRecalculationIT`, `SecurityIT` — Testcontainers integration tests that
  spin up a real PostgreSQL container. **Require a running Docker daemon.**

## Authentication

The API is protected with JWT bearer auth. You do **not** need to insert anything into the database
by hand — on first startup the app auto-seeds a single default admin account (see `AdminAccountSeeder`):

- username: `admin` (override with env var `RANKING_ADMIN_USERNAME`)
- password: `ChangeMe123!` (override with env var `RANKING_ADMIN_PASSWORD` — **set this before the
  first startup in any shared/deployed environment**, since the seeder only runs once, when no
  `ADMIN`-role player exists yet)

Log in to get a token:

```
POST /api/auth/login
{ "username": "admin", "password": "ChangeMe123!" }

-> { "token": "...", "playerId": 1, "username": "admin", "role": "ADMIN" }
```

Send the token on every subsequent request: `Authorization: Bearer <token>`.

- `POST /api/players` (add a club member) and everything under `/api/admin/**` require the `ADMIN`
  role.
- Every other `/api/**` endpoint just requires being logged in (any role).
- `POST /api/auth/login` and the Swagger UI routes are open.

Other JWT settings (`ranking.security.*` in `application.yml`): `jwt-secret` (override with
`RANKING_JWT_SECRET` — required for any real deployment, the default is dev-only) and
`jwt-expiration-minutes` (default 1440 = 24h).

### Adding club members

Only an admin can add members, via the same player-creation endpoint used before, now extended with
login credentials:

```
POST /api/players          (Authorization: Bearer <admin token>)
{
  "fullName": "홍길동",
  "email": "hong@example.com",
  "username": "hong",
  "password": "at-least-8-chars",
  "role": "MEMBER"          // optional, defaults to MEMBER; omit unless adding another admin
}
```

The new member can then log in with that `username`/`password` at `POST /api/auth/login`. There's no
self-signup endpoint by design — an admin (동아리장) enrolls each member.

## Scoring model

For each recorded match:

```
setMargin    = setsWonByWinner - setsWonByLoser         (e.g. 4-0 -> 4, 4-3 -> 1)
marginWeight = min(1 + setMargin * 0.1, ranking.margin-weight-cap)   (default cap 2.0)
tierWeight   = lookup(tier_weight_config, winnerTier, loserTier)     (admin-tunable, seeded 4x4 matrix)
winnerPoints = round(ranking.base-points * tierWeight * marginWeight)
loserPoints  = round(winnerPoints * ranking.loser-consolation-ratio) (default ratio 0 -> loser gets 0)
```

- Tiers (1 = best, 4 = worst) are **not** manually assigned. After every match, all players in that
  discipline (singles/doubles are tracked completely separately) are re-sorted by points and split
  into quartiles.
- New players have no ranking row until their first match in a discipline; they start at 0 points /
  tier 4 once recorded.
- Doubles rankings are per-individual (partners vary match to match), not per fixed team. The "team
  tier" used for scoring a doubles match is the rounded average of the two partners' tiers, and a
  winning pair each receive the full `winnerPoints` (not split).
- Every point award is recorded as a `PointTransaction` audit row (base/tier/margin weights, tiers at
  the time, points before/after) — see `GET /api/players/{id}/point-history`.
- Admins can retune the 16-cell tier weight matrix at runtime via
  `GET/PUT /api/admin/tier-weights` without a redeploy.

## Key endpoints

- `POST /api/auth/login`
- `POST /api/players` (ADMIN only), `GET /api/players`, `GET/PUT /api/players/{id}` (PUT is ADMIN only)
- `GET /api/players/{id}/rankings`, `GET /api/players/{id}/point-history`
- `POST /api/matches` — record a singles or doubles match (teams + per-set game scores)
- `GET /api/matches`, `GET /api/matches/{id}`
- `GET /api/leaderboards/singles`, `GET /api/leaderboards/doubles`
- `GET/PUT /api/admin/tier-weights`

Full request/response shapes are documented in Swagger UI once the app is running.
