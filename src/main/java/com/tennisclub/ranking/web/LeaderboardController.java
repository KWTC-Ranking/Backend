package com.tennisclub.ranking.web;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.dto.ranking.LeaderboardEntryResponse;
import com.tennisclub.ranking.service.LeaderboardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboards")
@RequiredArgsConstructor
public class LeaderboardController {

	private final LeaderboardService leaderboardService;

	@GetMapping("/singles")
	public List<LeaderboardEntryResponse> singles() {
		return leaderboardService.getLeaderboard(MatchType.SINGLES);
	}

	@GetMapping("/doubles")
	public List<LeaderboardEntryResponse> doubles() {
		return leaderboardService.getLeaderboard(MatchType.DOUBLES);
	}
}
