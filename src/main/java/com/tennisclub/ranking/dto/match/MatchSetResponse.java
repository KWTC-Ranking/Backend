package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.MatchSet;

public record MatchSetResponse(int setNumber, int teamAGames, int teamBGames) {

	public static MatchSetResponse from(MatchSet set) {
		return new MatchSetResponse(set.getSetNumber(), set.getTeamAGames(), set.getTeamBGames());
	}
}
