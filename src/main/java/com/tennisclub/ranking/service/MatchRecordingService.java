package com.tennisclub.ranking.service;

import com.tennisclub.ranking.config.RankingProperties;
import com.tennisclub.ranking.domain.Match;
import com.tennisclub.ranking.domain.MatchOutcome;
import com.tennisclub.ranking.domain.MatchSet;
import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchTeam;
import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.domain.PointTransaction;
import com.tennisclub.ranking.domain.TierWeightConfig;
import com.tennisclub.ranking.dto.match.MatchRecordRequest;
import com.tennisclub.ranking.dto.match.MatchResponse;
import com.tennisclub.ranking.dto.match.MatchSetRequest;
import com.tennisclub.ranking.dto.match.MatchTeamRequest;
import com.tennisclub.ranking.exception.InvalidMatchException;
import com.tennisclub.ranking.repository.MatchRepository;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import com.tennisclub.ranking.repository.PlayerRepository;
import com.tennisclub.ranking.repository.PointTransactionRepository;
import com.tennisclub.ranking.repository.TierWeightConfigRepository;
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

	@Transactional
	public MatchResponse recordMatch(MatchRecordRequest request) {
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

		TierWeightConfig tierWeightConfig = tierWeightConfigRepository
				.findByWinnerTierAndLoserTier(winnerTier, loserTier)
				.orElseThrow(() -> new IllegalStateException(
						"Missing tier weight configuration for winnerTier=" + winnerTier + ", loserTier=" + loserTier));

		ScoringService.ScoringResult result = scoringService.calculate(new ScoringService.ScoringInput(
				tierWeightConfig.getWeight(),
				setsWonByA > setsWonByB ? setsWonByA : setsWonByB,
				setsWonByA > setsWonByB ? setsWonByB : setsWonByA,
				rankingProperties.getBasePoints(),
				rankingProperties.getMarginWeightCap(),
				rankingProperties.getLoserConsolationRatio()));

		List<PointTransaction> transactions = new ArrayList<>();
		for (PlayerRanking ranking : winnerRankings) {
			transactions.add(applyOutcome(
					match, ranking, MatchOutcome.WINNER, result.winnerPointsEarned(), winnerTier, loserTier, result, tierWeightConfig));
		}
		for (PlayerRanking ranking : loserRankings) {
			transactions.add(applyOutcome(
					match, ranking, MatchOutcome.LOSER, result.loserPointsEarned(), winnerTier, loserTier, result, tierWeightConfig));
		}
		pointTransactionRepository.saveAll(transactions);

		tierRecalculationService.recalculateTiers(request.matchType());

		return MatchResponse.from(match, transactions);
	}

	private PointTransaction applyOutcome(
			Match match,
			PlayerRanking ranking,
			MatchOutcome role,
			int pointsAwarded,
			int winnerTier,
			int loserTier,
			ScoringService.ScoringResult result,
			TierWeightConfig tierWeightConfig) {
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
				.tierWeight(tierWeightConfig.getWeight())
				.marginWeight(result.marginWeight())
				.winnerTierAtMatch(winnerTier)
				.loserTierAtMatch(loserTier)
				.setMargin(result.setMargin())
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
}
