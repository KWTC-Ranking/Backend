package com.tennisclub.ranking.web;

import com.tennisclub.ranking.domain.Match;
import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.dto.match.MatchRecordRequest;
import com.tennisclub.ranking.dto.match.MatchResponse;
import com.tennisclub.ranking.dto.match.MatchSummaryResponse;
import com.tennisclub.ranking.exception.ResourceNotFoundException;
import com.tennisclub.ranking.repository.MatchRepository;
import com.tennisclub.ranking.repository.PointTransactionRepository;
import com.tennisclub.ranking.service.MatchRecordingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

	private final MatchRecordingService matchRecordingService;
	private final MatchRepository matchRepository;
	private final PointTransactionRepository pointTransactionRepository;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MatchResponse recordMatch(@Valid @RequestBody MatchRecordRequest request) {
		return matchRecordingService.recordMatch(request);
	}

	@GetMapping
	public Page<MatchSummaryResponse> listMatches(
			@RequestParam(required = false) MatchType matchType, @RequestParam(required = false) Long playerId, Pageable pageable) {
		return matchRepository.search(matchType, playerId, pageable).map(MatchSummaryResponse::from);
	}

	@GetMapping("/{id}")
	public MatchResponse getMatch(@PathVariable Long id) {
		Match match = matchRepository
				.findByIdWithDetails(id)
				.orElseThrow(() -> new ResourceNotFoundException("Match " + id + " not found"));
		return MatchResponse.from(match, pointTransactionRepository.findByMatchId(id));
	}
}
