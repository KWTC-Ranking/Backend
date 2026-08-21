package com.tennisclub.ranking.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "ranking")
@Validated
public class RankingProperties {

	@Positive
	private int basePoints = 100;

	@DecimalMin(value = "1.0")
	private BigDecimal marginWeightCap = new BigDecimal("2.0");

	/**
	 * Loser points = basePoints * tierWeight * (setsWonByLoser / totalSets) * this ratio — a
	 * shutout loss (0 sets won) always earns 0 regardless of this value; a close loss earns a
	 * meaningful fraction of what the winner got.
	 */
	@PositiveOrZero
	private BigDecimal loserConsolationRatio = new BigDecimal("0.5");

	/**
	 * Points-per-tier-step used to seed a brand-new player's initial points when an admin
	 * manually picks their starting tier (1=best..4=weakest) at creation time, instead of
	 * leaving them at the tier-4/0-point default. Seed points = (4 - tier) * tierSeedStep, so a
	 * tier-1 pick starts 3 steps above tier 4. This is a soft prior, not a pin: the next
	 * TierRecalculationService pass for that discipline re-sorts everyone by points as usual, so
	 * real match results gradually override the seed.
	 */
	@PositiveOrZero
	private int tierSeedStep = 300;

	public int getBasePoints() {
		return basePoints;
	}

	public void setBasePoints(int basePoints) {
		this.basePoints = basePoints;
	}

	public BigDecimal getMarginWeightCap() {
		return marginWeightCap;
	}

	public void setMarginWeightCap(BigDecimal marginWeightCap) {
		this.marginWeightCap = marginWeightCap;
	}

	public BigDecimal getLoserConsolationRatio() {
		return loserConsolationRatio;
	}

	public void setLoserConsolationRatio(BigDecimal loserConsolationRatio) {
		this.loserConsolationRatio = loserConsolationRatio;
	}

	public int getTierSeedStep() {
		return tierSeedStep;
	}

	public void setTierSeedStep(int tierSeedStep) {
		this.tierSeedStep = tierSeedStep;
	}
}
