package com.tennisclub.ranking.web;

import com.tennisclub.ranking.dto.admin.SeasonStateResponse;
import com.tennisclub.ranking.dto.admin.SeasonStateUpdateRequest;
import com.tennisclub.ranking.service.SeasonStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only: opens/closes the season (covered by the existing /api/admin/** hasRole("ADMIN") rule). */
@RestController
@RequestMapping("/api/admin/season")
@RequiredArgsConstructor
public class AdminSeasonController {

	private final SeasonStateService seasonStateService;

	@PutMapping
	public SeasonStateResponse updateState(@Valid @RequestBody SeasonStateUpdateRequest request) {
		return seasonStateService.updateState(request);
	}
}
