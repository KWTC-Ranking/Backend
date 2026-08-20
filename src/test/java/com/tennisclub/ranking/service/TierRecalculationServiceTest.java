package com.tennisclub.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TierRecalculationServiceTest {

	@Mock
	private PlayerRankingRepository playerRankingRepository;

	private TierRecalculationService newService() {
		return new TierRecalculationService(playerRankingRepository);
	}

	private List<PlayerRanking> rankingsOfSize(int n) {
		return IntStream.range(0, n)
				.mapToObj(i -> new PlayerRanking(new Player("Player " + i, null), MatchType.SINGLES))
				.toList();
	}

	private List<Integer> tiersOf(List<PlayerRanking> rankings) {
		return rankings.stream().map(PlayerRanking::getTier).toList();
	}

	@Test
	void emptyList_doesNothing() {
		when(playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(MatchType.SINGLES)).thenReturn(List.of());
		newService().recalculateTiers(MatchType.SINGLES);
		// no exception, nothing to assert beyond no interaction failures
	}

	@Test
	void singlePlayer_getsTierOne() {
		List<PlayerRanking> rankings = rankingsOfSize(1);
		when(playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(MatchType.SINGLES)).thenReturn(rankings);
		newService().recalculateTiers(MatchType.SINGLES);
		assertThat(tiersOf(rankings)).containsExactly(1);
	}

	@Test
	void twoPlayers_splitAcrossTierOneAndTierThree() {
		List<PlayerRanking> rankings = rankingsOfSize(2);
		when(playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(MatchType.SINGLES)).thenReturn(rankings);
		newService().recalculateTiers(MatchType.SINGLES);
		// tier = (i*4/n)+1 -> i=0: (0*4/2)+1=1 ; i=1: (1*4/2)+1=3
		assertThat(tiersOf(rankings)).containsExactly(1, 3);
	}

	@Test
	void threePlayers_expectedTierAssignment() {
		List<PlayerRanking> rankings = rankingsOfSize(3);
		when(playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(MatchType.SINGLES)).thenReturn(rankings);
		newService().recalculateTiers(MatchType.SINGLES);
		// i=0: (0*4/3)+1=1 ; i=1: (4/3=1)+1=2 ; i=2: (8/3=2)+1=3
		assertThat(tiersOf(rankings)).containsExactly(1, 2, 3);
	}

	@Test
	void fourPlayers_oneEachTier() {
		List<PlayerRanking> rankings = rankingsOfSize(4);
		when(playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(MatchType.SINGLES)).thenReturn(rankings);
		newService().recalculateTiers(MatchType.SINGLES);
		assertThat(tiersOf(rankings)).containsExactly(1, 2, 3, 4);
	}

	@Test
	void fivePlayers_expectedTierAssignment() {
		List<PlayerRanking> rankings = rankingsOfSize(5);
		when(playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(MatchType.SINGLES)).thenReturn(rankings);
		newService().recalculateTiers(MatchType.SINGLES);
		// i*4/5: 0,0,1,2,3 -> +1: 1,1,2,3,4
		assertThat(tiersOf(rankings)).containsExactly(1, 1, 2, 3, 4);
	}

	@Test
	void eightPlayers_evenSplitOfTwoPerTier() {
		List<PlayerRanking> rankings = rankingsOfSize(8);
		when(playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(MatchType.SINGLES)).thenReturn(rankings);
		newService().recalculateTiers(MatchType.SINGLES);
		assertThat(tiersOf(rankings)).containsExactly(1, 1, 2, 2, 3, 3, 4, 4);
	}

	@Test
	void hundredPlayers_evenQuartileSplit() {
		List<PlayerRanking> rankings = rankingsOfSize(100);
		when(playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(MatchType.SINGLES)).thenReturn(rankings);
		newService().recalculateTiers(MatchType.SINGLES);
		List<Integer> tiers = tiersOf(rankings);
		assertThat(tiers.subList(0, 25)).allMatch(t -> t == 1);
		assertThat(tiers.subList(25, 50)).allMatch(t -> t == 2);
		assertThat(tiers.subList(50, 75)).allMatch(t -> t == 3);
		assertThat(tiers.subList(75, 100)).allMatch(t -> t == 4);
	}
}
