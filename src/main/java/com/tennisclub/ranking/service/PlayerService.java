package com.tennisclub.ranking.service;

import com.tennisclub.ranking.config.RankingProperties;
import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.domain.PlayerRole;
import com.tennisclub.ranking.dto.player.PlayerCreateRequest;
import com.tennisclub.ranking.dto.player.PlayerUpdateRequest;
import com.tennisclub.ranking.dto.ranking.PointHistoryEntryResponse;
import com.tennisclub.ranking.exception.PlayerDeletionNotAllowedException;
import com.tennisclub.ranking.exception.ResourceNotFoundException;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import com.tennisclub.ranking.repository.PlayerRepository;
import com.tennisclub.ranking.repository.PointTransactionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {

	private static final int MAX_TIER = 4;

	private final PlayerRepository playerRepository;
	private final PlayerRankingRepository playerRankingRepository;
	private final PointTransactionRepository pointTransactionRepository;
	private final PasswordEncoder passwordEncoder;
	private final RankingProperties rankingProperties;

	@Transactional
	public Player createPlayer(PlayerCreateRequest request) {
		if (playerRepository.findByUsername(request.username()).isPresent()) {
			throw new IllegalArgumentException("Username '" + request.username() + "' is already taken");
		}
		PlayerRole role = request.role() != null ? request.role() : PlayerRole.MEMBER;
		String passwordHash = passwordEncoder.encode(request.password());
		Player player = playerRepository.save(
				new Player(request.fullName(), request.email(), request.username(), passwordHash, role));

		if (request.initialTier() != null) {
			seedRanking(player, MatchType.SINGLES, request.initialTier());
			seedRanking(player, MatchType.DOUBLES, request.initialTier());
		}

		return player;
	}

	/**
	 * Eagerly creates a PlayerRanking instead of waiting for the player's first match (see
	 * MatchRecordingService.findOrCreateRankings), seeded with points proportional to the
	 * admin-chosen tier so the very next tier recalculation places them in roughly the right
	 * quartile instead of at the bottom alongside every other 0-point player.
	 */
	private void seedRanking(Player player, MatchType matchType, int initialTier) {
		PlayerRanking ranking = new PlayerRanking(player, matchType);
		ranking.setTier(initialTier);
		ranking.setPoints((MAX_TIER - initialTier) * rankingProperties.getTierSeedStep());
		playerRankingRepository.save(ranking);
	}

	public Player getPlayer(Long id) {
		return playerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Player " + id + " not found"));
	}

	public List<Player> listPlayers(Boolean active) {
		if (active == null) {
			return playerRepository.findAll();
		}
		return playerRepository.findByActive(active);
	}

	@Transactional
	public Player updatePlayer(Long id, PlayerUpdateRequest request) {
		Player player = getPlayer(id);
		if (request.fullName() != null) {
			player.setFullName(request.fullName());
		}
		if (request.email() != null) {
			player.setEmail(request.email());
		}
		if (request.active() != null) {
			player.setActive(request.active());
		}
		return player;
	}

	/**
	 * Hard delete — only for players with no recorded match history (e.g. a test/mistaken
	 * account). A PlayerRanking row only exists once a player has played a match (see
	 * MatchRecordingService), so its presence is what we use to detect real history.
	 * Players who have played should be deactivated instead, not deleted, since their matches
	 * still need to reference a valid player.
	 */
	@Transactional
	public void deletePlayer(Long id) {
		Player player = getPlayer(id);
		if (!playerRankingRepository.findByPlayerId(id).isEmpty()) {
			throw new PlayerDeletionNotAllowedException(
					"Player " + id + " has recorded match history and cannot be deleted; deactivate instead");
		}
		playerRepository.delete(player);
	}

	public List<PlayerRanking> getRankings(Long playerId) {
		getPlayer(playerId);
		return playerRankingRepository.findByPlayerId(playerId);
	}

	public Page<PointHistoryEntryResponse> getPointHistory(Long playerId, MatchType matchType, Pageable pageable) {
		getPlayer(playerId);
		Page<com.tennisclub.ranking.domain.PointTransaction> page = matchType == null
				? pointTransactionRepository.findByPlayerIdOrderByCreatedAtDesc(playerId, pageable)
				: pointTransactionRepository.findByPlayerIdAndMatchTypeOrderByCreatedAtDesc(playerId, matchType, pageable);
		return page.map(PointHistoryEntryResponse::from);
	}

	/** Self-service password change — requires knowing the current password. */
	@Transactional
	public void changePassword(Long playerId, String currentPassword, String newPassword) {
		Player player = getPlayer(playerId);
		if (!passwordEncoder.matches(currentPassword, player.getPasswordHash())) {
			throw new BadCredentialsException("Current password is incorrect");
		}
		player.setPasswordHash(passwordEncoder.encode(newPassword));
	}

	/** Admin-triggered reset (e.g. a member forgot their password) — no current password needed. */
	@Transactional
	public void resetPassword(Long playerId, String newPassword) {
		Player player = getPlayer(playerId);
		player.setPasswordHash(passwordEncoder.encode(newPassword));
	}
}
