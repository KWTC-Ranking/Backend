package com.tennisclub.ranking.dto.player;

import com.tennisclub.ranking.domain.Player;
import java.time.Instant;

public record PlayerResponse(Long id, String fullName, String email, boolean active, Instant createdAt) {

	public static PlayerResponse from(Player player) {
		return new PlayerResponse(player.getId(), player.getFullName(), player.getEmail(), player.isActive(), player.getCreatedAt());
	}
}
