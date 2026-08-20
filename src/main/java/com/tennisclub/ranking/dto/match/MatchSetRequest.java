package com.tennisclub.ranking.dto.match;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record MatchSetRequest(@Positive int setNumber, @PositiveOrZero int teamAGames, @PositiveOrZero int teamBGames) {}
