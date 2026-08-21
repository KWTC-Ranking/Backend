-- Scoring margin now derives from games won (see ScoringService), not sets won: for a single-set
-- match -- the common case in this club -- setsWonByWinner/setsWonByLoser is always 1-0, so the old
-- set-based margin was constant and gave a shutout loss the same result as a narrow one. Renaming
-- to match what the column actually records now.
ALTER TABLE point_transaction RENAME COLUMN set_margin TO game_margin;
