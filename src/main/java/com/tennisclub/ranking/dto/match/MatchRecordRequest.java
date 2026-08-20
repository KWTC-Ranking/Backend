package com.tennisclub.ranking.dto.match;

import com.tennisclub.ranking.domain.MatchType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record MatchRecordRequest(
		@NotNull MatchType matchType,
		Instant playedAt,
		@NotNull @Size(min = 2, max = 2) List<@Valid MatchTeamRequest> teams,
		@NotEmpty List<@Valid MatchSetRequest> sets) {}
