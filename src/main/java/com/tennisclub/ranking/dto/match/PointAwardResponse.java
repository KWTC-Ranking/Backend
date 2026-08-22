package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.MatchOutcome;
import com.tennisclub.ranking.domain.PointTransaction;

public record PointAwardResponse(Long playerId, String fullName, MatchOutcome role, int pointsAwarded) {

	private static final String DELETED_PLAYER_LABEL = "(탈퇴한 회원)";

	public static PointAwardResponse from(PointTransaction transaction) {
		boolean playerDeleted = transaction.getPlayer() == null;
		return new PointAwardResponse(
				playerDeleted ? null : transaction.getPlayer().getId(),
				playerDeleted ? DELETED_PLAYER_LABEL : transaction.getPlayer().getFullName(),
				transaction.getRole(),
				transaction.getPointsAwarded());
	}
}
