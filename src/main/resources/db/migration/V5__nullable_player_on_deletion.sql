-- Allow a player to be permanently deleted without destroying the matches/point history they were
-- part of. Their own references become NULL ("탈퇴한 회원") instead of the whole row being blocked
-- or cascaded away, so opponents/partners keep their wins, losses and points.
ALTER TABLE match_team_player ALTER COLUMN player_id DROP NOT NULL;
ALTER TABLE match_team_player DROP CONSTRAINT match_team_player_player_id_fkey;
ALTER TABLE match_team_player
    ADD CONSTRAINT match_team_player_player_id_fkey
    FOREIGN KEY (player_id) REFERENCES player (id) ON DELETE SET NULL;

ALTER TABLE point_transaction ALTER COLUMN player_id DROP NOT NULL;
ALTER TABLE point_transaction DROP CONSTRAINT point_transaction_player_id_fkey;
ALTER TABLE point_transaction
    ADD CONSTRAINT point_transaction_player_id_fkey
    FOREIGN KEY (player_id) REFERENCES player (id) ON DELETE SET NULL;
