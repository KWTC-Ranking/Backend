package com.tennisclub.ranking.web;

import com.tennisclub.ranking.dto.admin.SeasonStateResponse;
import com.tennisclub.ranking.service.SeasonStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only, open to any authenticated member (not just admins) so the frontend can show a
 * "시즌 마감중" banner / disable match submission without needing admin access.
 */
@RestController
@RequestMapping("/api/season")
@RequiredArgsConstructor
public class SeasonController {

	private final SeasonStateService seasonStateService;

	@GetMapping
	public SeasonStateResponse getState() {
		return seasonStateService.getState();
	}
}
