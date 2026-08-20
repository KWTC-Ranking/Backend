package com.tennisclub.ranking.dto.ranking;

public record LeaderboardEntryResponse(int rank, Long playerId, String fullName, int points, int tier, int wins, int losses) {}
