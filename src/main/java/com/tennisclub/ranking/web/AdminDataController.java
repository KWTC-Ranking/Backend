package com.tennisclub.ranking.web;

import com.tennisclub.ranking.dto.admin.DataResetResponse;
import com.tennisclub.ranking.service.AdminDataResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/data")
@RequiredArgsConstructor
public class AdminDataController {

	private final AdminDataResetService adminDataResetService;

	/**
	 * Deletes every match, ranking and non-admin player. Meant to be run once, right before
	 * going live, to clear out everything created while testing. Irreversible.
	 */
	@PostMapping("/reset-test-data")
	public DataResetResponse resetTestData() {
		return adminDataResetService.resetTestData();
	}
}
