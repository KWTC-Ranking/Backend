package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.Match;
import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchTeam;
import com.tennisclub.ranking.domain.MatchType;
import java.time.Instant;
import java.util.Comparator;

public record MatchSummaryResponse(
		Long id, MatchType matchType, Instant playedAt, MatchSide winningSide, String teamASummary, String teamBSummary) {

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
				.sorted(Comparator.comparing(tp -> tp.getPlayer().getFullName()))
				.map(tp -> tp.getPlayer().getFullName())
				.reduce((a, b) -> a + " / " + b)
				.orElse("");
		return names + " (" + team.getSetsWon() + " sets)";
	}
}
