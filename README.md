# Tennis Club Ranking Service

Spring Boot backend for a tennis club's singles/doubles rankings. Tracks matches, computes ranking
points with a tier + margin-weighted formula, and auto-requartiles players into 4 tiers per discipline.

## Stack

- Java 21, Spring Boot 3.3, Gradle (Groovy DSL)
- Spring Web, Spring Data JPA, Bean Validation
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
- `MatchRecordingServiceIT`, `TierRecalculationIT` — Testcontainers integration tests that spin up a
  real PostgreSQL container. **Require a running Docker daemon.**

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

No authentication/authorization is implemented — this MVP assumes a trusted, internal-use deployment.

## Key endpoints

- `POST /api/players`, `GET /api/players`, `GET/PUT /api/players/{id}`
- `GET /api/players/{id}/rankings`, `GET /api/players/{id}/point-history`
- `POST /api/matches` — record a singles or doubles match (teams + per-set game scores)
- `GET /api/matches`, `GET /api/matches/{id}`
- `GET /api/leaderboards/singles`, `GET /api/leaderboards/doubles`
- `GET/PUT /api/admin/tier-weights`

Full request/response shapes are documented in Swagger UI once the app is running.
