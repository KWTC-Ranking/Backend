package com.tennisclub.ranking.web;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.dto.player.PasswordResetRequest;
import com.tennisclub.ranking.dto.player.PlayerCreateRequest;
import com.tennisclub.ranking.dto.player.PlayerRankingResponse;
import com.tennisclub.ranking.dto.player.PlayerResponse;
import com.tennisclub.ranking.dto.player.PlayerUpdateRequest;
import com.tennisclub.ranking.dto.ranking.PointHistoryEntryResponse;
import com.tennisclub.ranking.service.PlayerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

	private final PlayerService playerService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PlayerResponse createPlayer(@Valid @RequestBody PlayerCreateRequest request) {
		return PlayerResponse.from(playerService.createPlayer(request));
	}

	@GetMapping
	public List<PlayerResponse> listPlayers(@RequestParam(required = false) Boolean active) {
		return playerService.listPlayers(active).stream().map(PlayerResponse::from).toList();
	}

	@GetMapping("/{id}")
	public PlayerResponse getPlayer(@PathVariable Long id) {
		return PlayerResponse.from(playerService.getPlayer(id));
	}

	@PutMapping("/{id}")
	public PlayerResponse updatePlayer(@PathVariable Long id, @Valid @RequestBody PlayerUpdateRequest request) {
		return PlayerResponse.from(playerService.updatePlayer(id, request));
	}

	@GetMapping("/{id}/rankings")
	public List<PlayerRankingResponse> getRankings(@PathVariable Long id) {
		return playerService.getRankings(id).stream().map(PlayerRankingResponse::from).toList();
	}

	@GetMapping("/{id}/point-history")
	public Page<PointHistoryEntryResponse> getPointHistory(
			@PathVariable Long id, @RequestParam(required = false) MatchType matchType, Pageable pageable) {
		return playerService.getPointHistory(id, matchType, pageable);
	}

	/**
	 * Admin-only: permanently delete a player. Only allowed when the player has no recorded
	 * match history (e.g. a test account or one added by mistake) — a 409 is returned otherwise,
	 * telling the caller to deactivate instead via PUT.
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
		playerService.deletePlayer(id);
		return ResponseEntity.noContent().build();
	}

	/** Admin-only: reset a member's password (e.g. they forgot it) without needing the old one. */
	@PutMapping("/{id}/password")
	public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
		playerService.resetPassword(id, request.newPassword());
		return ResponseEntity.noContent().build();
	}
}
