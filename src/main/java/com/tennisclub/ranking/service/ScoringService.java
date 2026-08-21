package com.tennisclub.ranking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Pure scoring calculator: no Spring/JPA dependencies beyond {@code @Service} for injection.
 * winnerPoints = round(basePoints * winnerTierWeight * marginWeight), marginWeight capped.
 * loserPoints  = round(basePoints * loserTierWeight * (gamesWonByLoser / totalGames) * loserConsolationRatio)
 *
 * Margin is measured in GAMES, not sets. Most matches here are a single set, where
 * setsWonByWinner/setsWonByLoser is always a flat 1-0 — using that for the margin would make
 * marginWeight constant and loserPoints always 0 regardless of how close the set actually was
 * (e.g. 4-3 vs 4-0). Games capture real closeness regardless of how many sets a match has.
 * setsWonByWinner/setsWonByLoser are kept only as an input sanity check (see calculate()) — the
 * structural "who won" call already happened in MatchRecordingService.
 *
 * winnerTierWeight and loserTierWeight are looked up in OPPOSITE directions from the same 4x4
 * tier_weight_config matrix: winnerTierWeight = lookup(winnerTier, loserTier) rewards the winner
 * for an upset; loserTierWeight = lookup(loserTier, winnerTier) rewards the LOSER for how close
 * they came to an upset of their own (e.g. a tier-3 player nearly beating a tier-1 player gets the
 * same generous weight a tier-3 player would get for actually beating a tier-1 player). Reusing
 * the winner's weight for the loser would do the opposite — an underdog's close loss would be
 * scaled down by the *favorite's* low win-weight instead of rewarded.
 */
@Service
public class ScoringService {

	private static final BigDecimal MARGIN_WEIGHT_STEP = new BigDecimal("0.1");
	private static final BigDecimal ONE = BigDecimal.ONE;

	public record ScoringInput(
			BigDecimal winnerTierWeight,
			BigDecimal loserTierWeight,
			int setsWonByWinner,
			int setsWonByLoser,
			int gamesWonByWinner,
			int gamesWonByLoser,
			int basePoints,
			BigDecimal marginWeightCap,
			BigDecimal loserConsolationRatio) {}

	public record ScoringResult(int gameMargin, BigDecimal marginWeight, int winnerPointsEarned, int loserPointsEarned) {}

	public ScoringResult calculate(ScoringInput input) {
		if (input.setsWonByWinner() <= input.setsWonByLoser()) {
			throw new IllegalArgumentException("Winner must have won more sets than the loser");
		}

		int gameMargin = input.gamesWonByWinner() - input.gamesWonByLoser();

		BigDecimal marginWeightRaw = ONE.add(MARGIN_WEIGHT_STEP.multiply(BigDecimal.valueOf(gameMargin)));
		// .max(ONE) guards a pathological case (e.g. a meaningless extra set played after the match
		// was already decided, skewing the loser's total games above the winner's) from ever giving
		// the winner less than the base weight.
		BigDecimal marginWeight = marginWeightRaw.min(input.marginWeightCap()).max(ONE);

		BigDecimal winnerPointsRaw = BigDecimal.valueOf(input.basePoints())
				.multiply(input.winnerTierWeight())
				.multiply(marginWeight);
		int winnerPointsEarned = winnerPointsRaw.setScale(0, RoundingMode.HALF_UP).intValueExact();

		int totalGames = input.gamesWonByWinner() + input.gamesWonByLoser();
		BigDecimal loserGameShare = BigDecimal.valueOf(input.gamesWonByLoser())
				.divide(BigDecimal.valueOf(totalGames), 6, RoundingMode.HALF_UP);
		BigDecimal loserPointsRaw = BigDecimal.valueOf(input.basePoints())
				.multiply(input.loserTierWeight())
				.multiply(loserGameShare)
				.multiply(input.loserConsolationRatio());
		int loserPointsEarned = loserPointsRaw.setScale(0, RoundingMode.HALF_UP).intValueExact();

		return new ScoringResult(gameMargin, marginWeight, winnerPointsEarned, loserPointsEarned);
	}
}
