package com.tennisclub.ranking.dto.player;

import com.tennisclub.ranking.domain.PlayerRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PlayerUpdateRequest(
		@Size(max = 150) String fullName, @Email String email, Boolean active, PlayerRole role) {}
