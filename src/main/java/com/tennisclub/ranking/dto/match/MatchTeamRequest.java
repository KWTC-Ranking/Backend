package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.MatchSide;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MatchTeamRequest(@NotNull MatchSide side, @NotEmpty List<Long> playerIds) {}
