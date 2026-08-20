package com.tennisclub.ranking.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TierWeightEntryRequest(
		@Min(1) @Max(4) int winnerTier, @Min(1) @Max(4) int loserTier, @NotNull @DecimalMin("0.01") BigDecimal weight) {}
