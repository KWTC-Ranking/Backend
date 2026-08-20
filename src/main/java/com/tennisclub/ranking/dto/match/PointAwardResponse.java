package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.MatchOutcome;
import com.tennisclub.ranking.domain.PointTransaction;

public record PointAwardResponse(Long playerId, String fullName, MatchOutcome role, int pointsAwarded) {

	public static PointAwardResponse from(PointTransaction transaction) {
		return new PointAwardResponse(
				transaction.getPlayer().getId(),
				transaction.getPlayer().getFullName(),
				transaction.getRole(),
				transaction.getPointsAwarded());
	}
}
