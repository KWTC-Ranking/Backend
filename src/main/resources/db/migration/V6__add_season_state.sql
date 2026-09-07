-- Single-row switch admins use to open/close the "season" (e.g. a monthly 2-week focused
-- training window). While closed, new match submissions are rejected but every read endpoint
-- (leaderboards, match history, point history) keeps working as normal.
CREATE TABLE season_state (
    id SMALLINT PRIMARY KEY DEFAULT 1,
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_season_state_singleton CHECK (id = 1)
);

INSERT INTO season_state (id, is_open) VALUES (1, TRUE);
