package com.tennisclub.ranking.dto.match;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Corrects a mistakenly recorded set score. Teams/players/matchType cannot be changed — only the sets. */
public record MatchScoreCorrectionRequest(@NotEmpty List<@Valid MatchSetRequest> sets) {}
