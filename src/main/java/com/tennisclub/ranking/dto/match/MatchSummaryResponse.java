package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.Match;
import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchTeam;
import com.tennisclub.ranking.domain.MatchType;
import java.time.Instant;

public record MatchSummaryResponse(
		Long id, MatchType matchType, Instant playedAt, MatchSide winningSide, String teamASummary, String teamBSummary) {

	private static final String DELETED_PLAYER_LABEL = "(탈퇴한 회원)";

	public static MatchSummaryResponse from(Match match) {
		String teamA = summarize(match, MatchSide.A);
		String teamB = summarize(match, MatchSide.B);
		return new MatchSummaryResponse(match.getId(), match.getMatchType(), match.getPlayedAt(), match.getWinningSide(), teamA, teamB);
	}

	private static String summarize(Match match, MatchSide side) {
		return match.getTeams().stream()
				.filter(team -> team.getSide() == side)
				.findFirst()
				.map(MatchSummaryResponse::describeTeam)
				.orElse("");
	}

	private static String describeTeam(MatchTeam team) {
		String names = team.getPlayers().stream()
				.map(tp -> tp.getPlayer() != null ? tp.getPlayer().getFullName() : DELETED_PLAYER_LABEL)
				.sorted()
				.reduce((a, b) -> a + " / " + b)
				.orElse("");
		return names + " (" + team.getSetsWon() + " sets)";
	}
}
