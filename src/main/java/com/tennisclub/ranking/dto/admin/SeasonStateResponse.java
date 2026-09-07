package com.tennisclub.ranking.dto.admin;

import com.tennisclub.ranking.domain.SeasonState;
import java.time.Instant;

public record SeasonStateResponse(boolean open, Instant updatedAt) {

	public static SeasonStateResponse from(SeasonState state) {
		return new SeasonStateResponse(state.isOpen(), state.getUpdatedAt());
	}
}
