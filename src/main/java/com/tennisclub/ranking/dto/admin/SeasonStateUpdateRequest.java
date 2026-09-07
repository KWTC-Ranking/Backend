package com.tennisclub.ranking.dto.admin;

import jakarta.validation.constraints.NotNull;

public record SeasonStateUpdateRequest(@NotNull Boolean open) {}
