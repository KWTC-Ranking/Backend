package com.tennisclub.ranking.service;

import com.tennisclub.ranking.config.RankingProperties;
import com.tennisclub.ranking.domain.Match;
import com.tennisclub.ranking.domain.MatchOutcome;
import com.tennisclub.ranking.domain.MatchSet;
import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchTeam;
import com.tennisclub.ranking.domain.MatchTeamPlayer;
import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.domain.PointTransaction;
import com.tennisclub.ranking.domain.TierWeightConfig;
import com.tennisclub.ranking.dto.match.MatchRecordRequest;
import com.tennisclub.ranking.dto.match.MatchResponse;
import com.tennisclub.ranking.dto.match.MatchScoreCorrectionRequest;
import com.tennisclub.ranking.dto.match.MatchSetRequest;
import com.tennisclub.ranking.dto.match.MatchTeamRequest;
import com.tennisclub.ranking.exception.InvalidMatchException;
import com.tennisclub.ranking.exception.ResourceNotFoundException;
import com.tennisclub.ranking.repository.MatchRepository;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import com.tennisclub.ranking.repository.PlayerRepository;
import com.tennisclub.ranking.repository.PointTransactionRepository;
import com.tennisclub.ranking.repository.TierWeightConfigRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchRecordingService {

	private static final int SINGLES_PLAYERS_PER_TEAM = 1;
	private static final int DOUBLES_PLAYERS_PER_TEAM = 2;

	private final PlayerRepository playerRepository;
	private final PlayerRankingRepository playerRankingRepository;
	private final MatchRepository matchRepository;
	private final TierWeightConfigRepository tierWeightConfigRepository;
	private final PointTransactionRepository pointTransactionRepository;
	private final ScoringService scoringService;
	private final TierRecalculationService tierRecalculationService;
	private final RankingProperties rankingProperties;
	private final SeasonStateService seasonStateService;

	/**
	 * Records a brand-new match. Gated on the season being open (see SeasonStateService) — admin
	 * corrections/deletions of existing matches (below) are NOT gated, only new submissions.
	 */
	@Transactional
	public MatchResponse recordMatch(MatchRecordRequest request) {
		seasonStateService.requireOpen();

		MatchTeamRequest teamARequest = requireSide(request, MatchSide.A);
		MatchTeamRequest teamBRequest = requireSide(request, MatchSide.B);

		int expectedPlayersPerTeam = request.matchType() == MatchType.SINGLES ? SINGLES_PLAYERS_PER_TEAM : DOUBLES_PLAYERS_PER_TEAM;
		validateTeamSize(teamARequest, expectedPlayersPerTeam);
		validateTeamSize(teamBRequest, expectedPlayersPerTeam);

		List<Long> allPlayerIds = new ArrayList<>();
		allPlayerIds.addAll(teamARequest.playerIds());
		allPlayerIds.addAll(teamBRequest.playerIds());
		if (new HashSet<>(allPlayerIds).size() != allPlayerIds.size()) {
			throw new InvalidMatchException("A player cannot appear more than once in the same match");
		}

		Map<Long, Player> playersById = loadActivePlayers(allPlayerIds);

		validateSets(request.sets());
		int setsWonByA = countSetsWon(request.sets(), true);
		int setsWonByB = countSetsWon(request.sets(), false);
		if (setsWonByA == setsWonByB) {
			throw new InvalidMatchException("A match cannot end in a draw");
		}
		MatchSide winningSide = setsWonByA > setsWonByB ? MatchSide.A : MatchSide.B;
		int gamesWonByA = sumGames(request.sets(), true);
		int gamesWonByB = sumGames(request.sets(), false);

		Match match = new Match(request.matchType(), request.playedAt() != null ? request.playedAt() : Instant.now());
		match.setWinningSide(winningSide);

		MatchTeam teamA = new MatchTeam(MatchSide.A, setsWonByA);
		teamARequest.playerIds().forEach(id -> teamA.addPlayer(playersById.get(id)));
		match.addTeam(teamA);

		MatchTeam teamB = new MatchTeam(MatchSide.B, setsWonByB);
		teamBRequest.playerIds().forEach(id -> teamB.addPlayer(playersById.get(id)));
		match.addTeam(teamB);

		int setNumber = 1;
		for (MatchSetRequest setRequest : request.sets()) {
			match.addSet(new MatchSet(setNumber++, setRequest.teamAGames(), setRequest.teamBGames()));
		}

		match = matchRepository.save(match);

		MatchTeam winningTeam = winningSide == MatchSide.A ? teamA : teamB;
		MatchTeam losingTeam = winningSide == MatchSide.A ? teamB : teamA;

		List<PlayerRanking> winnerRankings = findOrCreateRankings(winningTeam.getPlayers().stream()
				.map(tp -> tp.getPlayer())
				.toList(), request.matchType());
		List<PlayerRanking> loserRankings = findOrCreateRankings(losingTeam.getPlayers().stream()
				.map(tp -> tp.getPlayer())
				.toList(), request.matchType());

		int winnerTier = teamTier(winnerRankings);
		int loserTier = teamTier(loserRankings);

		// winnerTierWeight rewards the winner for the upset they pulled off; loserTierWeight is
		// looked up in the OPPOSITE direction so the loser is rewarded for how close *they* came to
		// an upset, instead of being scaled by the favorite's low win-weight (see ScoringService).
		TierWeightConfig winnerTierWeightConfig = tierWeightConfigRepository
				.findByWinnerTierAndLoserTier(winnerTier, loserTier)
				.orElseThrow(() -> new IllegalStateException(
						"Missing tier weight configuration for winnerTier=" + winnerTier + ", loserTier=" + loserTier));
		TierWeightConfig loserTierWeightConfig = tierWeightConfigRepository
				.findByWinnerTierAndLoserTier(loserTier, winnerTier)
				.orElseThrow(() -> new IllegalStateException(
						"Missing tier weight configuration for winnerTier=" + loserTier + ", loserTier=" + winnerTier));

		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				winnerTierWeightConfig.getWeight(),
				loserTierWeightConfig.getWeight(),
				setsWonByA > setsWonByB ? setsWonByA : setsWonByB,
				setsWonByA > setsWonByB ? setsWonByB : setsWonByA,
				winningSide == MatchSide.A ? gamesWonByA : gamesWonByB,
				winningSide == MatchSide.A ? gamesWonByB : gamesWonByA,
				rankingProperties.getBasePoints(),
				rankingProperties.getMarginWeightCap(),
				rankingProperties.getLoserConsolationRatio()));

		List<PointTransaction> transactions = new ArrayList<>();
		for (PlayerRanking ranking : winnerRankings) {
			transactions.add(applyOutcome(
					match, ranking, MatchOutcome.WINNER, result.winnerPointsEarned(), winnerTier, loserTier, result,
					winnerTierWeightConfig.getWeight()));
		}
		for (PlayerRanking ranking : loserRankings) {
			transactions.add(applyOutcome(
					match, ranking, MatchOutcome.LOSER, result.loserPointsEarned(), winnerTier, loserTier, result,
					loserTierWeightConfig.getWeight()));
		}
		pointTransactionRepository.saveAll(transactions);

		tierRecalculationService.recalculateTiers(request.matchType());

		return MatchResponse.from(match, transactions);
	}

	/**
	 * Corrects a mistakenly entered set score for an already-recorded match. Teams/players cannot
	 * change (only the sets), but the winning side can flip if the correction reverses the outcome.
	 *
	 * The old point award is undone (points/wins/losses subtracted back out) and a fresh one is
	 * applied using each player's CURRENT tier — not the tier at the original match time — since
	 * that's the only tier snapshot the system keeps live. For a club-sized dataset corrections are
	 * expected to happen shortly after the mistake, so this is a reasonable approximation.
	 */
	@Transactional
	public MatchResponse correctScore(Long matchId, MatchScoreCorrectionRequest request) {
		Match match = matchRepository
				.findByIdWithDetails(matchId)
				.orElseThrow(() -> new ResourceNotFoundException("Match " + matchId + " not found"));

		validateSets(request.sets());
		int newSetsWonByA = countSetsWon(request.sets(), true);
		int newSetsWonByB = countSetsWon(request.sets(), false);
		if (newSetsWonByA == newSetsWonByB) {
			throw new InvalidMatchException("A match cannot end in a draw");
		}
		MatchSide newWinningSide = newSetsWonByA > newSetsWonByB ? MatchSide.A : MatchSide.B;
		int newGamesWonByA = sumGames(request.sets(), true);
		int newGamesWonByB = sumGames(request.sets(), false);

		MatchTeam teamA = findTeam(match, MatchSide.A);
		MatchTeam teamB = findTeam(match, MatchSide.B);
		if (hasDeletedPlayer(teamA) || hasDeletedPlayer(teamB)) {
			throw new InvalidMatchException(
					"Match " + matchId + " has a deleted participant and its score can no longer be corrected");
		}

		List<PointTransaction> oldTransactions = pointTransactionRepository.findByMatchId(matchId);
		for (PointTransaction oldTransaction : oldTransactions) {
			PlayerRanking ranking = playerRankingRepository
					.findByPlayerIdAndMatchType(oldTransaction.getPlayer().getId(), match.getMatchType())
					.orElseThrow(() -> new IllegalStateException(
							"Ranking missing for player " + oldTransaction.getPlayer().getId() + " while correcting match " + matchId));
			ranking.setPoints(ranking.getPoints() - oldTransaction.getPointsAwarded());
			if (oldTransaction.getRole() == MatchOutcome.WINNER) {
				ranking.setWins(ranking.getWins() - 1);
			} else {
				ranking.setLosses(ranking.getLosses() - 1);
			}
		}
		pointTransactionRepository.deleteAll(oldTransactions);

		// Flush the removal before re-adding: otherwise Hibernate can order the new sets' INSERTs
		// before the old sets' DELETEs within the same flush and trip the (match_id, set_number)
		// unique constraint when a set number is reused.
		match.getSets().clear();
		matchRepository.flush();
		int setNumber = 1;
		for (MatchSetRequest setRequest : request.sets()) {
			match.addSet(new MatchSet(setNumber++, setRequest.teamAGames(), setRequest.teamBGames()));
		}
		match.setWinningSide(newWinningSide);
		teamA.setSetsWon(newSetsWonByA);
		teamB.setSetsWon(newSetsWonByB);

		MatchTeam winningTeam = newWinningSide == MatchSide.A ? teamA : teamB;
		MatchTeam losingTeam = newWinningSide == MatchSide.A ? teamB : teamA;

		List<PlayerRanking> winnerRankings = findOrCreateRankings(
				winningTeam.getPlayers().stream().map(MatchTeamPlayer::getPlayer).toList(), match.getMatchType());
		List<PlayerRanking> loserRankings = findOrCreateRankings(
				losingTeam.getPlayers().stream().map(MatchTeamPlayer::getPlayer).toList(), match.getMatchType());

		int winnerTier = teamTier(winnerRankings);
		int loserTier = teamTier(loserRankings);

		TierWeightConfig winnerTierWeightConfig = tierWeightConfigRepository
				.findByWinnerTierAndLoserTier(winnerTier, loserTier)
				.orElseThrow(() -> new IllegalStateException(
						"Missing tier weight configuration for winnerTier=" + winnerTier + ", loserTier=" + loserTier));
		TierWeightConfig loserTierWeightConfig = tierWeightConfigRepository
				.findByWinnerTierAndLoserTier(loserTier, winnerTier)
				.orElseThrow(() -> new IllegalStateException(
						"Missing tier weight configuration for winnerTier=" + loserTier + ", loserTier=" + winnerTier));

		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				winnerTierWeightConfig.getWeight(),
				loserTierWeightConfig.getWeight(),
				newSetsWonByA > newSetsWonByB ? newSetsWonByA : newSetsWonByB,
				newSetsWonByA > newSetsWonByB ? newSetsWonByB : newSetsWonByA,
				newWinningSide == MatchSide.A ? newGamesWonByA : newGamesWonByB,
				newWinningSide == MatchSide.A ? newGamesWonByB : newGamesWonByA,
				rankingProperties.getBasePoints(),
				rankingProperties.getMarginWeightCap(),
				rankingProperties.getLoserConsolationRatio()));

		List<PointTransaction> newTransactions = new ArrayList<>();
		for (PlayerRanking ranking : winnerRankings) {
			newTransactions.add(applyOutcome(
					match, ranking, MatchOutcome.WINNER, result.winnerPointsEarned(), winnerTier, loserTier, result,
					winnerTierWeightConfig.getWeight()));
		}
		for (PlayerRanking ranking : loserRankings) {
			newTransactions.add(applyOutcome(
					match, ranking, MatchOutcome.LOSER, result.loserPointsEarned(), winnerTier, loserTier, result,
					loserTierWeightConfig.getWeight()));
		}
		pointTransactionRepository.saveAll(newTransactions);

		tierRecalculationService.recalculateTiers(match.getMatchType());

		return MatchResponse.from(match, newTransactions);
	}

	/**
	 * Permanently deletes a mistakenly recorded match. The points/wins/losses it awarded are
	 * undone for every participant who still has a live account (a participant whose account was
	 * since deleted has no ranking left to undo — see PlayerService.deletePlayer). The match row
	 * itself, its teams/sets, and its point transactions are removed by the DB's ON DELETE CASCADE
	 * (see V1__init_schema.sql) once matchRepository.delete(match) runs.
	 */
	@Transactional
	public void deleteMatch(Long matchId) {
		Match match = matchRepository
				.findByIdWithDetails(matchId)
				.orElseThrow(() -> new ResourceNotFoundException("Match " + matchId + " not found"));

		for (PointTransaction transaction : pointTransactionRepository.findByMatchId(matchId)) {
			if (transaction.getPlayer() == null) {
				continue;
			}
			playerRankingRepository
					.findByPlayerIdAndMatchType(transaction.getPlayer().getId(), match.getMatchType())
					.ifPresent(ranking -> {
						ranking.setPoints(ranking.getPoints() - transaction.getPointsAwarded());
						if (transaction.getRole() == MatchOutcome.WINNER) {
							ranking.setWins(ranking.getWins() - 1);
						} else {
							ranking.setLosses(ranking.getLosses() - 1);
						}
					});
		}

		MatchType matchType = match.getMatchType();
		matchRepository.delete(match);
		tierRecalculationService.recalculateTiers(matchType);
	}

	private MatchTeam findTeam(Match match, MatchSide side) {
		return match.getTeams().stream()
				.filter(team -> team.getSide() == side)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Match " + match.getId() + " is missing team " + side));
	}

	private boolean hasDeletedPlayer(MatchTeam team) {
		return team.getPlayers().stream().anyMatch(tp -> tp.getPlayer() == null);
	}

	private PointTransaction applyOutcome(
			Match match,
			PlayerRanking ranking,
			MatchOutcome role,
			int pointsAwarded,
			int winnerTier,
			int loserTier,
			ScoringService.ScoringResult result,
			BigDecimal tierWeightUsed) {
		int pointsBefore = ranking.getPoints();
		ranking.setPoints(pointsBefore + pointsAwarded);
		if (role == MatchOutcome.WINNER) {
			ranking.setWins(ranking.getWins() + 1);
		} else {
			ranking.setLosses(ranking.getLosses() + 1);
		}

		return PointTransaction.builder()
				.player(ranking.getPlayer())
				.match(match)
				.matchType(match.getMatchType())
				.role(role)
				.basePoints(rankingProperties.getBasePoints())
				.tierWeight(tierWeightUsed)
				.marginWeight(result.marginWeight())
				.winnerTierAtMatch(winnerTier)
				.loserTierAtMatch(loserTier)
				.gameMargin(result.gameMargin())
				.pointsBefore(pointsBefore)
				.pointsAfter(ranking.getPoints())
				.pointsAwarded(pointsAwarded)
				.build();
	}

	private List<PlayerRanking> findOrCreateRankings(List<Player> players, MatchType matchType) {
		List<PlayerRanking> rankings = new ArrayList<>();
		for (Player player : players) {
			PlayerRanking ranking = playerRankingRepository
					.findByPlayerIdAndMatchType(player.getId(), matchType)
					.orElseGet(() -> playerRankingRepository.save(new PlayerRanking(player, matchType)));
			rankings.add(ranking);
		}
		return rankings;
	}

	private int teamTier(List<PlayerRanking> rankings) {
		if (rankings.size() == 1) {
			return rankings.get(0).getTier();
		}
		double average = rankings.stream().mapToInt(PlayerRanking::getTier).average().orElseThrow();
		return (int) Math.round(average);
	}

	private MatchTeamRequest requireSide(MatchRecordRequest request, MatchSide side) {
		return request.teams().stream()
				.filter(team -> team.side() == side)
				.findFirst()
				.orElseThrow(() -> new InvalidMatchException("Match must include exactly one team for side " + side));
	}

	private void validateTeamSize(MatchTeamRequest team, int expectedPlayersPerTeam) {
		if (team.playerIds().size() != expectedPlayersPerTeam) {
			throw new InvalidMatchException(
					"Team " + team.side() + " must have exactly " + expectedPlayersPerTeam + " player(s)");
		}
	}

	private Map<Long, Player> loadActivePlayers(List<Long> playerIds) {
		Set<Long> uniqueIds = new HashSet<>(playerIds);
		List<Player> players = playerRepository.findAllById(uniqueIds);
		if (players.size() != uniqueIds.size()) {
			throw new InvalidMatchException("One or more players could not be found");
		}
		Map<Long, Player> byId = new LinkedHashMap<>();
		for (Player player : players) {
			if (!player.isActive()) {
				throw new InvalidMatchException("Player " + player.getId() + " is not active");
			}
			byId.put(player.getId(), player);
		}
		return byId;
	}

	private void validateSets(List<MatchSetRequest> sets) {
		if (sets.isEmpty()) {
			throw new InvalidMatchException("At least one set is required");
		}
		for (MatchSetRequest set : sets) {
			if (set.teamAGames() == set.teamBGames()) {
				throw new InvalidMatchException("Set " + set.setNumber() + " cannot be tied");
			}
		}
	}

	private int countSetsWon(List<MatchSetRequest> sets, boolean forTeamA) {
		return (int) sets.stream()
				.filter(set -> forTeamA ? set.teamAGames() > set.teamBGames() : set.teamBGames() > set.teamAGames())
				.count();
	}

	private int sumGames(List<MatchSetRequest> sets, boolean forTeamA) {
		return sets.stream().mapToInt(forTeamA ? MatchSetRequest::teamAGames : MatchSetRequest::teamBGames).sum();
	}
}
