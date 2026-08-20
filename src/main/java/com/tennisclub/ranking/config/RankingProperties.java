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

	@PositiveOrZero
	private BigDecimal loserConsolationRatio = BigDecimal.ZERO;

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
}
