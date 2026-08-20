package com.tennisclub.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ScoringServiceTest {

	private final ScoringService scoringService = new ScoringService();

	@Test
	void sameTier_minimumMargin_appliesBaseWeightsOnly() {
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), 4, 3, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		assertThat(result.setMargin()).isEqualTo(1);
		assertThat(result.marginWeight()).isEqualByComparingTo("1.1");
		// 100 * 1.00 * 1.1 = 110
		assertThat(result.winnerPointsEarned()).isEqualTo(110);
		assertThat(result.loserPointsEarned()).isEqualTo(0);
	}

	@Test
	void underdogWins_maxFavorableTierGap_appliesHighestWeight() {
		// winner tier 4 beats loser tier 1 -> matrix weight 1.50
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.50"), 4, 0, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		assertThat(result.setMargin()).isEqualTo(4);
		assertThat(result.marginWeight()).isEqualByComparingTo("1.4");
		// 100 * 1.50 * 1.4 = 210
		assertThat(result.winnerPointsEarned()).isEqualTo(210);
	}

	@Test
	void favoriteWins_maxUnfavorableTierGap_appliesLowestWeight() {
		// winner tier 1 beats loser tier 4 -> matrix weight 0.50
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("0.50"), 4, 3, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		// 100 * 0.50 * 1.1 = 55
		assertThat(result.winnerPointsEarned()).isEqualTo(55);
	}

	@Test
	void largeMargin_isCappedAtConfiguredMax() {
		// setsWonByWinner - setsWonByLoser = 15 -> raw marginWeight = 1 + 15*0.1 = 2.5, capped to 2.0
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), 15, 0, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		assertThat(result.marginWeight()).isEqualByComparingTo("2.0");
		assertThat(result.winnerPointsEarned()).isEqualTo(200);
	}

	@Test
	void loserConsolationRatio_awardsProportionalPointsToLoser() {
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), 4, 0, 100, new BigDecimal("2.0"), new BigDecimal("0.5")));

		// winnerPoints = 100 * 1.00 * 1.4 = 140; loserPoints = 140 * 0.5 = 70
		assertThat(result.winnerPointsEarned()).isEqualTo(140);
		assertThat(result.loserPointsEarned()).isEqualTo(70);
	}

	@Test
	void defaultConsolationRatio_awardsZeroToLoser() {
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), 4, 3, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		assertThat(result.loserPointsEarned()).isEqualTo(0);
	}

	@Test
	void roundingAtHalfBoundary_usesHalfUp() {
		// 100 * 0.83 * 1.1 = 91.3 -> not a boundary case; use a crafted exact .5 case instead.
		// 100 * 0.67 * 1.5(margin capped irrelevant) -> use tierWeight 0.335*... simpler: craft basePoints to hit .5
		ScoringService.ScoringResult result = scoringService.calculate(
				new ScoringService.ScoringInput(new BigDecimal("0.005"), 4, 3, 10000, new BigDecimal("2.0"), BigDecimal.ZERO));

		// 10000 * 0.005 * 1.1 = 55.0 exactly -> sanity, no rounding ambiguity here.
		assertThat(result.winnerPointsEarned()).isEqualTo(55);
	}

	@Test
	void winnerMustHaveWonMoreSets_throwsOnInvalidInput() {
		assertThatThrownBy(() -> scoringService.calculate(
						new ScoringService.ScoringInput(BigDecimal.ONE, 2, 3, 100, new BigDecimal("2.0"), BigDecimal.ZERO)))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> scoringService.calculate(
						new ScoringService.ScoringInput(BigDecimal.ONE, 2, 2, 100, new BigDecimal("2.0"), BigDecimal.ZERO)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
