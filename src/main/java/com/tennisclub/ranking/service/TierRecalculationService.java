package com.tennisclub.ranking.service;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Requartiles every player's tier (1..4) within a discipline by ranking points:
 * top 25% -> tier 1, ... bottom 25% -> tier 4, using tier = (index * 4 / n) + 1
 * over players sorted by points DESC (ties broken by id ASC for determinism).
 */
@Service
@RequiredArgsConstructor
public class TierRecalculationService {

	private static final int TIER_COUNT = 4;

	private final PlayerRankingRepository playerRankingRepository;

	@Transactional
	public void recalculateTiers(MatchType matchType) {
		List<PlayerRanking> rankings = playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(matchType);
		int n = rankings.size();
		if (n == 0) {
			return;
		}

		for (int i = 0; i < n; i++) {
			int tier = (i * TIER_COUNT / n) + 1;
			rankings.get(i).setTier(tier);
		}
	}
}
