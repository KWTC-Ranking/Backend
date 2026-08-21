package com.tennisclub.ranking.dto.ranking;

import com.tennisclub.ranking.domain.MatchOutcome;
import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.PointTransaction;
import java.math.BigDecimal;
import java.time.Instant;

public record PointHistoryEntryResponse(
		Long id,
		Long matchId,
		MatchType matchType,
		MatchOutcome role,
		int basePoints,
		BigDecimal tierWeight,
		BigDecimal marginWeight,
		int winnerTierAtMatch,
		int loserTierAtMatch,
		int gameMargin,
		int pointsBefore,
		int pointsAfter,
		int pointsAwarded,
		Instant createdAt) {

	public static PointHistoryEntryResponse from(PointTransaction transaction) {
		return new PointHistoryEntryResponse(
				transaction.getId(),
				transaction.getMatch().getId(),
				transaction.getMatchType(),
				transaction.getRole(),
				transaction.getBasePoints(),
				transaction.getTierWeight(),
				transaction.getMarginWeight(),
				transaction.getWinnerTierAtMatch(),
				transaction.getLoserTierAtMatch(),
				transaction.getGameMargin(),
				transaction.getPointsBefore(),
				transaction.getPointsAfter(),
				transaction.getPointsAwarded(),
				transaction.getCreatedAt());
	}
}
