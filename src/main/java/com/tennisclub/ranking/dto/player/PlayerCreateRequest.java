package com.tennisclub.ranking.dto.player;

import com.tennisclub.ranking.domain.PlayerRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerCreateRequest(
		@NotBlank @Size(max = 150) String fullName,
		@Email String email,
		@NotBlank @Size(min = 3, max = 50) String username,
		@NotBlank @Size(min = 8, max = 100) String password,
		PlayerRole role,
		/** 1 (best) .. 4 (weakest). Null keeps the old lazy default (tier 4 on first match). */
		@Min(1) @Max(4) Integer initialTier) {}
