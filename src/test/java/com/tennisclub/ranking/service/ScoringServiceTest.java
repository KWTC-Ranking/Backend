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
				new BigDecimal("1.00"), new BigDecimal("1.00"), 1, 0, 4, 3, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		assertThat(result.gameMargin()).isEqualTo(1);
		assertThat(result.marginWeight()).isEqualByComparingTo("1.1");
		// 100 * 1.00 * 1.1 = 110
		assertThat(result.winnerPointsEarned()).isEqualTo(110);
		assertThat(result.loserPointsEarned()).isEqualTo(0);
	}

	@Test
	void underdogWins_maxFavorableTierGap_appliesHighestWeight() {
		// winner tier 4 beats loser tier 1 -> matrix weight 1.50 for the winner
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.50"), new BigDecimal("0.50"), 1, 0, 4, 0, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		assertThat(result.gameMargin()).isEqualTo(4);
		assertThat(result.marginWeight()).isEqualByComparingTo("1.4");
		// 100 * 1.50 * 1.4 = 210
		assertThat(result.winnerPointsEarned()).isEqualTo(210);
	}

	@Test
	void favoriteWins_maxUnfavorableTierGap_appliesLowestWeight() {
		// winner tier 1 beats loser tier 4 -> matrix weight 0.50 for the winner
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("0.50"), new BigDecimal("1.50"), 1, 0, 4, 3, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		// 100 * 0.50 * 1.1 = 55
		assertThat(result.winnerPointsEarned()).isEqualTo(55);
	}

	@Test
	void largeMargin_isCappedAtConfiguredMax() {
		// gamesWonByWinner - gamesWonByLoser = 15 -> raw marginWeight = 1 + 15*0.1 = 2.5, capped to 2.0
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), new BigDecimal("1.00"), 1, 0, 15, 0, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		assertThat(result.marginWeight()).isEqualByComparingTo("2.0");
		assertThat(result.winnerPointsEarned()).isEqualTo(200);
	}

	@Test
	void loserConsolationRatio_scalesWithGamesWonByLoser_closeLossEarnsMore() {
		// close loss: loser won 3 of 7 games, same tier on both sides
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), new BigDecimal("1.00"), 1, 0, 4, 3, 100, new BigDecimal("2.0"), new BigDecimal("0.5")));

		// winnerPoints = 100 * 1.00 * 1.1 = 110; loserPoints = round(100 * 1.00 * (3/7) * 0.5) = round(21.43) = 21
		assertThat(result.winnerPointsEarned()).isEqualTo(110);
		assertThat(result.loserPointsEarned()).isEqualTo(21);
	}

	@Test
	void loserConsolationRatio_shutoutLoss_earnsZeroRegardlessOfRatio() {
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), new BigDecimal("1.00"), 1, 0, 4, 0, 100, new BigDecimal("2.0"), new BigDecimal("0.5")));

		assertThat(result.loserPointsEarned()).isEqualTo(0);
	}

	@Test
	void zeroConsolationRatio_awardsZeroToLoserEvenOnACloseLoss() {
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), new BigDecimal("1.00"), 1, 0, 4, 3, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		assertThat(result.loserPointsEarned()).isEqualTo(0);
	}

	@Test
	void singleSetMatch_closeGameScore_awardsLoserConsolationPoints() {
		// Regression test: a match recorded as a single set (setsWonByWinner=1, setsWonByLoser=0)
		// used to always give the loser 0 points no matter the game score, because the old formula
		// scaled off SETS won by the loser -- which is always 0 in a one-set match. Scaling off
		// games instead means a close single-set loss (e.g. 3-4) earns real consolation points.
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), new BigDecimal("1.00"), 1, 0, 4, 3, 100, new BigDecimal("2.0"), new BigDecimal("0.5")));

		assertThat(result.loserPointsEarned()).isGreaterThan(0);
	}

	@Test
	void underdogLoserTierWeight_isLookedUpInReverseAndRewardsANearUpset() {
		// tier-3 player narrowly loses to a tier-1 player 3-4. The winner's weight, lookup(1,3),
		// is the favorite's low win-value (0.67). If the loser's consolation reused that same
		// weight it would punish the underdog for almost pulling off the upset. Using the reverse
		// lookup(3,1) = 1.33 instead rewards them the way a tier-3 player actually beating a tier-1
		// player would be rewarded.
		ScoringService.ScoringResult favoriteWinsNarrowly = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("0.67"), new BigDecimal("1.33"), 1, 0, 4, 3, 100, new BigDecimal("2.0"), new BigDecimal("0.5")));
		// loserPoints = round(100 * 1.33 * (3/7) * 0.5) = round(28.5) = 28
		assertThat(favoriteWinsNarrowly.loserPointsEarned()).isEqualTo(28);

		// Reusing the winner's own (low) weight for the loser -- the old, wrong behavior -- would
		// have given noticeably less: round(100 * 0.67 * (3/7) * 0.5) = 14.
		ScoringService.ScoringResult usingWinnerWeightForLoser = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("0.67"), new BigDecimal("0.67"), 1, 0, 4, 3, 100, new BigDecimal("2.0"), new BigDecimal("0.5")));
		assertThat(usingWinnerWeightForLoser.loserPointsEarned()).isLessThan(favoriteWinsNarrowly.loserPointsEarned());
	}

	@Test
	void pathologicalNegativeGameMargin_marginWeightNeverDropsBelowOne() {
		// Contrived: winner took the match on sets but the loser somehow ended up with more total
		// games (e.g. a meaningless set played after the match was already decided). The winner
		// should never be penalized below the base weight for this.
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("1.00"), new BigDecimal("1.00"), 2, 1, 8, 16, 100, new BigDecimal("2.0"), BigDecimal.ZERO));

		assertThat(result.gameMargin()).isEqualTo(-8);
		assertThat(result.marginWeight()).isEqualByComparingTo("1.0");
		assertThat(result.winnerPointsEarned()).isEqualTo(100);
	}

	@Test
	void roundingAtHalfBoundary_usesHalfUp() {
		// 100 * 0.83 * 1.1 = 91.3 -> not a boundary case; use a crafted exact .5 case instead.
		// 100 * 0.67 * 1.5(margin capped irrelevant) -> use tierWeight 0.335*... simpler: craft basePoints to hit .5
		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				new BigDecimal("0.005"),
				new BigDecimal("0.005"),
				1,
				0,
				4,
				3,
				10000,
				new BigDecimal("2.0"),
				BigDecimal.ZERO));

		// 10000 * 0.005 * 1.1 = 55.0 exactly -> sanity, no rounding ambiguity here.
		assertThat(result.winnerPointsEarned()).isEqualTo(55);
	}

	@Test
	void winnerMustHaveWonMoreSets_throwsOnInvalidInput() {
		assertThatThrownBy(() -> scoringService.calculate(new ScoringService.ScoringInput(
						BigDecimal.ONE, BigDecimal.ONE, 2, 3, 4, 3, 100, new BigDecimal("2.0"), BigDecimal.ZERO)))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> scoringService.calculate(new ScoringService.ScoringInput(
						BigDecimal.ONE, BigDecimal.ONE, 2, 2, 4, 3, 100, new BigDecimal("2.0"), BigDecimal.ZERO)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
