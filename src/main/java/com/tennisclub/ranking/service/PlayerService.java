package com.tennisclub.ranking.service;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.dto.player.PlayerCreateRequest;
import com.tennisclub.ranking.dto.player.PlayerUpdateRequest;
import com.tennisclub.ranking.dto.ranking.PointHistoryEntryResponse;
import com.tennisclub.ranking.exception.ResourceNotFoundException;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import com.tennisclub.ranking.repository.PlayerRepository;
import com.tennisclub.ranking.repository.PointTransactionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {

	private final PlayerRepository playerRepository;
	private final PlayerRankingRepository playerRankingRepository;
	private final PointTransactionRepository pointTransactionRepository;

	@Transactional
	public Player createPlayer(PlayerCreateRequest request) {
		return playerRepository.save(new Player(request.fullName(), request.email()));
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
}
