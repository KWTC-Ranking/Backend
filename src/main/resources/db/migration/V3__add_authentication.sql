ALTER TABLE player
    ADD COLUMN username      VARCHAR(50) NOT NULL,
    ADD COLUMN password_hash VARCHAR(255) NOT NULL,
    ADD COLUMN role          VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('ADMIN', 'MEMBER'));

ALTER TABLE player ADD CONSTRAINT uq_player_username UNIQUE (username);
