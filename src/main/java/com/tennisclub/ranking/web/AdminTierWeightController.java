package com.tennisclub.ranking.web;

import com.tennisclub.ranking.dto.admin.TierWeightMatrixResponse;
import com.tennisclub.ranking.dto.admin.TierWeightUpdateRequest;
import com.tennisclub.ranking.service.TierWeightConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tier-weights")
@RequiredArgsConstructor
public class AdminTierWeightController {

	private final TierWeightConfigService tierWeightConfigService;

	@GetMapping
	public TierWeightMatrixResponse getMatrix() {
		return tierWeightConfigService.getMatrix();
	}

	@PutMapping
	public TierWeightMatrixResponse updateMatrix(@Valid @RequestBody TierWeightUpdateRequest request) {
		return tierWeightConfigService.updateMatrix(request);
	}
}
