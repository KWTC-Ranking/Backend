CREATE TABLE player (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    email       VARCHAR(255) UNIQUE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE player_ranking (
    id          BIGSERIAL PRIMARY KEY,
    player_id   BIGINT NOT NULL REFERENCES player (id),
    match_type  VARCHAR(20) NOT NULL CHECK (match_type IN ('SINGLES', 'DOUBLES')),
    points      INTEGER NOT NULL DEFAULT 0,
    tier        INTEGER NOT NULL DEFAULT 4 CHECK (tier BETWEEN 1 AND 4),
    wins        INTEGER NOT NULL DEFAULT 0,
    losses      INTEGER NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_player_ranking_player_match_type UNIQUE (player_id, match_type)
);

CREATE INDEX idx_player_ranking_leaderboard ON player_ranking (match_type, points DESC);

CREATE TABLE match (
    id            BIGSERIAL PRIMARY KEY,
    match_type    VARCHAR(20) NOT NULL CHECK (match_type IN ('SINGLES', 'DOUBLES')),
    played_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    winning_side  CHAR(1) NOT NULL CHECK (winning_side IN ('A', 'B')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE match_team (
    id        BIGSERIAL PRIMARY KEY,
    match_id  BIGINT NOT NULL REFERENCES match (id) ON DELETE CASCADE,
    side      CHAR(1) NOT NULL CHECK (side IN ('A', 'B')),
    sets_won  INTEGER NOT NULL,
    CONSTRAINT uq_match_team_match_side UNIQUE (match_id, side)
);

CREATE TABLE match_team_player (
    id             BIGSERIAL PRIMARY KEY,
    match_team_id  BIGINT NOT NULL REFERENCES match_team (id) ON DELETE CASCADE,
    player_id      BIGINT NOT NULL REFERENCES player (id),
    CONSTRAINT uq_match_team_player UNIQUE (match_team_id, player_id)
);

CREATE INDEX idx_match_team_player_player ON match_team_player (player_id);

CREATE TABLE match_set (
    id             BIGSERIAL PRIMARY KEY,
    match_id       BIGINT NOT NULL REFERENCES match (id) ON DELETE CASCADE,
    set_number     INTEGER NOT NULL,
    team_a_games   INTEGER NOT NULL,
    team_b_games   INTEGER NOT NULL,
    CONSTRAINT uq_match_set_match_number UNIQUE (match_id, set_number),
    CONSTRAINT ck_match_set_no_tie CHECK (team_a_games <> team_b_games)
);

CREATE TABLE tier_weight_config (
    id           BIGSERIAL PRIMARY KEY,
    winner_tier  INTEGER NOT NULL CHECK (winner_tier BETWEEN 1 AND 4),
    loser_tier   INTEGER NOT NULL CHECK (loser_tier BETWEEN 1 AND 4),
    weight       NUMERIC(4, 2) NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tier_weight_config_pair UNIQUE (winner_tier, loser_tier)
);

CREATE TABLE point_transaction (
    id                     BIGSERIAL PRIMARY KEY,
    player_id              BIGINT NOT NULL REFERENCES player (id),
    match_id               BIGINT NOT NULL REFERENCES match (id) ON DELETE CASCADE,
    match_type             VARCHAR(20) NOT NULL CHECK (match_type IN ('SINGLES', 'DOUBLES')),
    role                   VARCHAR(10) NOT NULL CHECK (role IN ('WINNER', 'LOSER')),
    base_points            INTEGER NOT NULL,
    tier_weight            NUMERIC(4, 2) NOT NULL,
    margin_weight          NUMERIC(4, 2) NOT NULL,
    winner_tier_at_match   INTEGER NOT NULL,
    loser_tier_at_match    INTEGER NOT NULL,
    set_margin             INTEGER NOT NULL,
    points_before          INTEGER NOT NULL,
    points_after           INTEGER NOT NULL,
    points_awarded         INTEGER NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_point_transaction_player ON point_transaction (player_id, created_at DESC);
