package com.tennisclub.ranking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Pure scoring calculator: no Spring/JPA dependencies beyond {@code @Service} for injection.
 * winnerPoints = round(basePoints * tierWeight * marginWeight), marginWeight capped.
 */
@Service
public class ScoringService {

	private static final BigDecimal MARGIN_WEIGHT_STEP = new BigDecimal("0.1");
	private static final BigDecimal ONE = BigDecimal.ONE;

	public record ScoringInput(
			BigDecimal tierWeight,
			int setsWonByWinner,
			int setsWonByLoser,
			int basePoints,
			BigDecimal marginWeightCap,
			BigDecimal loserConsolationRatio) {}

	public record ScoringResult(int setMargin, BigDecimal marginWeight, int winnerPointsEarned, int loserPointsEarned) {}

	public ScoringResult calculate(ScoringInput input) {
		if (input.setsWonByWinner() <= input.setsWonByLoser()) {
			throw new IllegalArgumentException("Winner must have won more sets than the loser");
		}

		int setMargin = input.setsWonByWinner() - input.setsWonByLoser();

		BigDecimal marginWeightRaw = ONE.add(MARGIN_WEIGHT_STEP.multiply(BigDecimal.valueOf(setMargin)));
		BigDecimal marginWeight = marginWeightRaw.min(input.marginWeightCap());

		BigDecimal winnerPointsRaw = BigDecimal.valueOf(input.basePoints())
				.multiply(input.tierWeight())
				.multiply(marginWeight);
		int winnerPointsEarned = winnerPointsRaw.setScale(0, RoundingMode.HALF_UP).intValueExact();

		BigDecimal loserPointsRaw = BigDecimal.valueOf(winnerPointsEarned).multiply(input.loserConsolationRatio());
		int loserPointsEarned = loserPointsRaw.setScale(0, RoundingMode.HALF_UP).intValueExact();

		return new ScoringResult(setMargin, marginWeight, winnerPointsEarned, loserPointsEarned);
	}
}
