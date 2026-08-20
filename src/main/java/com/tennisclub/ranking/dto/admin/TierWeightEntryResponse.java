package com.tennisclub.ranking.dto.admin;

import com.tennisclub.ranking.domain.TierWeightConfig;
import java.math.BigDecimal;

public record TierWeightEntryResponse(int winnerTier, int loserTier, BigDecimal weight) {

	public static TierWeightEntryResponse from(TierWeightConfig config) {
		return new TierWeightEntryResponse(config.getWinnerTier(), config.getLoserTier(), config.getWeight());
	}
}
