package com.tennisclub.ranking.dto.player;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.PlayerRanking;
import java.time.Instant;

public record PlayerRankingResponse(MatchType matchType, int points, int tier, int wins, int losses, Instant updatedAt) {

	public static PlayerRankingResponse from(PlayerRanking ranking) {
		return new PlayerRankingResponse(
				ranking.getMatchType(),
				ranking.getPoints(),
				ranking.getTier(),
				ranking.getWins(),
				ranking.getLosses(),
				ranking.getUpdatedAt());
	}
}
