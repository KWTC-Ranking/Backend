package com.tennisclub.ranking.service;

import com.tennisclub.ranking.domain.TierWeightConfig;
import com.tennisclub.ranking.dto.admin.TierWeightEntryRequest;
import com.tennisclub.ranking.dto.admin.TierWeightEntryResponse;
import com.tennisclub.ranking.dto.admin.TierWeightMatrixResponse;
import com.tennisclub.ranking.dto.admin.TierWeightUpdateRequest;
import com.tennisclub.ranking.exception.ResourceNotFoundException;
import com.tennisclub.ranking.repository.TierWeightConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TierWeightConfigService {

	private final TierWeightConfigRepository tierWeightConfigRepository;

	public TierWeightMatrixResponse getMatrix() {
		return new TierWeightMatrixResponse(tierWeightConfigRepository.findAllByOrderByWinnerTierAscLoserTierAsc().stream()
				.map(TierWeightEntryResponse::from)
				.toList());
	}

	@Transactional
	public TierWeightMatrixResponse updateMatrix(TierWeightUpdateRequest request) {
		for (TierWeightEntryRequest entry : request.entries()) {
			TierWeightConfig config = tierWeightConfigRepository
					.findByWinnerTierAndLoserTier(entry.winnerTier(), entry.loserTier())
					.orElseThrow(() -> new ResourceNotFoundException(
							"No tier weight entry for winnerTier=" + entry.winnerTier() + ", loserTier=" + entry.loserTier()));
			config.setWeight(entry.weight());
		}
		return getMatrix();
	}
}
