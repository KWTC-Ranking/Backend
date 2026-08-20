package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.Match;
import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.PointTransaction;
import java.time.Instant;
import java.util.List;

public record MatchResponse(
		Long id,
		MatchType matchType,
		Instant playedAt,
		MatchSide winningSide,
		List<MatchTeamResponse> teams,
		List<MatchSetResponse> sets,
		List<PointAwardResponse> pointTransactions) {

	public static MatchResponse from(Match match, List<PointTransaction> transactions) {
		List<MatchTeamResponse> teamResponses = match.getTeams().stream().map(MatchTeamResponse::from).toList();
		List<MatchSetResponse> setResponses = match.getSets().stream().map(MatchSetResponse::from).toList();
		List<PointAwardResponse> awardResponses = transactions.stream().map(PointAwardResponse::from).toList();
		return new MatchResponse(
				match.getId(), match.getMatchType(), match.getPlayedAt(), match.getWinningSide(), teamResponses, setResponses, awardResponses);
	}
}
