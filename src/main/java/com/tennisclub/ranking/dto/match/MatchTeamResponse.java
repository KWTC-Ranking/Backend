package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchTeam;
import java.util.List;

public record MatchTeamResponse(MatchSide side, int setsWon, List<PlayerRef> players) {

	private static final String DELETED_PLAYER_LABEL = "(탈퇴한 회원)";

	public record PlayerRef(Long playerId, String fullName) {}

	public static MatchTeamResponse from(MatchTeam team) {
		List<PlayerRef> refs = team.getPlayers().stream()
				.map(tp -> tp.getPlayer() != null
						? new PlayerRef(tp.getPlayer().getId(), tp.getPlayer().getFullName())
						: new PlayerRef(null, DELETED_PLAYER_LABEL))
				.toList();
		return new MatchTeamResponse(team.getSide(), team.getSetsWon(), refs);
	}
}
