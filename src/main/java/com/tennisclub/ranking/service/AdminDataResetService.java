package com.tennisclub.ranking.service;

import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRole;
import com.tennisclub.ranking.dto.admin.DataResetResponse;
import com.tennisclub.ranking.repository.MatchRepository;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import com.tennisclub.ranking.repository.PlayerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDataResetService {

	private final MatchRepository matchRepository;
	private final PlayerRankingRepository playerRankingRepository;
	private final PlayerRepository playerRepository;

	/**
	 * Wipes all match/ranking data and every non-admin player, so a club can clear out test
	 * accounts before going live with real members. ADMIN accounts are kept so whoever runs this
	 * doesn't lock themselves out.
	 *
	 * Deletion order matters: match rows are deleted first, which cascades at the DB level to
	 * match_team -> match_team_player / match_set, and to point_transaction (all declared
	 * ON DELETE CASCADE from match_id in V1__init_schema.sql). Only after those player_id
	 * references are gone can player_ranking and player rows be deleted without violating their
	 * (non-cascading) FKs.
	 */
	@Transactional
	public DataResetResponse resetTestData() {
		long deletedMatches = matchRepository.count();
		matchRepository.deleteAllInBatch();

		long deletedRankings = playerRankingRepository.count();
		playerRankingRepository.deleteAllInBatch();

		List<Player> nonAdmins = playerRepository.findByRoleNot(PlayerRole.ADMIN);
		playerRepository.deleteAll(nonAdmins);

		return new DataResetResponse(nonAdmins.size(), deletedMatches, deletedRankings);
	}
}
