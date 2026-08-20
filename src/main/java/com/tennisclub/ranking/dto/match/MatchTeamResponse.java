package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchTeam;
import java.util.List;

public record MatchTeamResponse(MatchSide side, int setsWon, List<PlayerRef> players) {

	public record PlayerRef(Long playerId, String fullName) {}

	public static MatchTeamResponse from(MatchTeam team) {
		List<PlayerRef> refs = team.getPlayers().stream()
				.map(tp -> new PlayerRef(tp.getPlayer().getId(), tp.getPlayer().getFullName()))
				.toList();
		return new MatchTeamResponse(team.getSide(), team.getSetsWon(), refs);
	}
}
