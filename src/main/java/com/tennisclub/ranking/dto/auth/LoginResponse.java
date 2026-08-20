package com.tennisclub.ranking.dto.auth;

import com.tennisclub.ranking.domain.PlayerRole;

public record LoginResponse(String token, Long playerId, String username, PlayerRole role) {}
