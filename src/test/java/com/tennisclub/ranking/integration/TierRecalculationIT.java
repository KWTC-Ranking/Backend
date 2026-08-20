package com.tennisclub.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import com.tennisclub.ranking.repository.PlayerRepository;
import com.tennisclub.ranking.service.TierRecalculationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TierRecalculationIT extends AbstractIntegrationTest {

	@Autowired
	private TierRecalculationService tierRecalculationService;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private PlayerRankingRepository playerRankingRepository;

	@Test
	void recalculateTiers_persistsQuartileSplitAcrossEightPlayers() {
		for (int i = 0; i < 8; i++) {
			Player player = playerRepository.save(new Player("Player" + i, null));
			PlayerRanking ranking = playerRankingRepository.save(new PlayerRanking(player, MatchType.SINGLES));
			ranking.setPoints((8 - i) * 10);
		}

		tierRecalculationService.recalculateTiers(MatchType.SINGLES);

		List<PlayerRanking> rankings = playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(MatchType.SINGLES);
		List<Integer> tiers = rankings.stream().map(PlayerRanking::getTier).toList();
		assertThat(tiers).containsExactly(1, 1, 2, 2, 3, 3, 4, 4);
	}
}
