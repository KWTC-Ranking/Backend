package com.tennisclub.ranking.service;

import com.tennisclub.ranking.domain.SeasonState;
import com.tennisclub.ranking.dto.admin.SeasonStateResponse;
import com.tennisclub.ranking.dto.admin.SeasonStateUpdateRequest;
import com.tennisclub.ranking.exception.SeasonClosedException;
import com.tennisclub.ranking.repository.SeasonStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeasonStateService {

	private final SeasonStateRepository seasonStateRepository;

	public SeasonStateResponse getState() {
		return SeasonStateResponse.from(loadState());
	}

	@Transactional
	public SeasonStateResponse updateState(SeasonStateUpdateRequest request) {
		SeasonState state = loadState();
		state.setOpen(request.open());
		return SeasonStateResponse.from(state);
	}

	/**
	 * Called by MatchRecordingService before accepting a new match. Admin corrections/deletions
	 * of already-recorded matches are NOT gated by this — only brand-new submissions are.
	 */
	public void requireOpen() {
		if (!loadState().isOpen()) {
			throw new SeasonClosedException("Season is currently closed — new match results are not being accepted");
		}
	}

	private SeasonState loadState() {
		return seasonStateRepository
				.findById(SeasonState.SINGLETON_ID)
				.orElseThrow(() -> new IllegalStateException("season_state row is missing (V6 migration should have seeded it)"));
	}
}
