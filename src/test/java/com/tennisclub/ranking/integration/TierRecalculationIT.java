package com.tennisclub.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.domain.PlayerRole;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import com.tennisclub.ranking.repository.PlayerRepository;
import com.tennisclub.ranking.service.TierRecalculationService;
import java.util.ArrayList;
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
		// Asserts against these same (still-managed, same-transaction) entity references rather
		// than re-querying "all SINGLES rankings" — the shared test database can carry rows from
		// other, non-transactional integration tests (e.g. MatchQueryIT), so a global re-query
		// would make this test's exact-count assertion flaky depending on run order.
		List<PlayerRanking> myRankings = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			Player player = playerRepository.save(new Player("TierRecalcPlayer" + i, null, "tierplayer" + i, "hash", PlayerRole.MEMBER));
			PlayerRanking ranking = playerRankingRepository.save(new PlayerRanking(player, MatchType.DOUBLES));
			ranking.setPoints((8 - i) * 10);
			myRankings.add(ranking);
		}

		tierRecalculationService.recalculateTiers(MatchType.DOUBLES);

		List<Integer> tiers = myRankings.stream().map(PlayerRanking::getTier).toList();
		assertThat(tiers).containsExactly(1, 1, 2, 2, 3, 3, 4, 4);
	}
}
