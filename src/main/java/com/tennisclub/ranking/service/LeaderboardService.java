package com.tennisclub.ranking.service;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.dto.ranking.LeaderboardEntryResponse;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

	private final PlayerRankingRepository playerRankingRepository;

	public List<LeaderboardEntryResponse> getLeaderboard(MatchType matchType) {
		List<PlayerRanking> rankings = playerRankingRepository.findByMatchTypeOrderByPointsDescIdAsc(matchType);
		List<LeaderboardEntryResponse> entries = new ArrayList<>();
		int rank = 1;
		for (PlayerRanking ranking : rankings) {
			entries.add(new LeaderboardEntryResponse(
					rank++,
					ranking.getPlayer().getId(),
					ranking.getPlayer().getFullName(),
					ranking.getPoints(),
					ranking.getTier(),
					ranking.getWins(),
					ranking.getLosses()));
		}
		return entries;
	}
}
