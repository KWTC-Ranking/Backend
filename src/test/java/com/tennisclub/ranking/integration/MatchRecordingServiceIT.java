package com.tennisclub.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRole;
import com.tennisclub.ranking.domain.PlayerRanking;
import com.tennisclub.ranking.dto.match.MatchRecordRequest;
import com.tennisclub.ranking.dto.match.MatchResponse;
import com.tennisclub.ranking.dto.match.MatchScoreCorrectionRequest;
import com.tennisclub.ranking.dto.match.MatchSetRequest;
import com.tennisclub.ranking.dto.match.MatchTeamRequest;
import com.tennisclub.ranking.exception.InvalidMatchException;
import com.tennisclub.ranking.repository.PlayerRankingRepository;
import com.tennisclub.ranking.repository.PlayerRepository;
import com.tennisclub.ranking.repository.PointTransactionRepository;
import com.tennisclub.ranking.service.MatchRecordingService;
import com.tennisclub.ranking.service.PlayerService;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MatchRecordingServiceIT extends AbstractIntegrationTest {

	@Autowired
	private MatchRecordingService matchRecordingService;

	@Autowired
	private PlayerService playerService;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private PlayerRankingRepository playerRankingRepository;

	@Autowired
	private PointTransactionRepository pointTransactionRepository;

	private Player player(String name) {
		String username = name.toLowerCase() + "-" + System.nanoTime();
		return playerRepository.save(new Player(name, null, username, "hash", PlayerRole.MEMBER));
	}

	@Test
	void singlesMatch_updatesWinnerAndLoserRankings() {
		Player winner = player("Alice");
		Player loser = player("Bob");

		// full shutout (0 games won by the loser in every set) so loser points stay 0 -- the
		// close-loss / partial-games case is covered separately by
		// singlesMatch_closeLoss_awardsLoserProportionalConsolationPoints below.
		MatchRecordRequest request = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(winner.getId())), new MatchTeamRequest(MatchSide.B, List.of(loser.getId()))),
				List.of(
						new MatchSetRequest(1, 4, 0),
						new MatchSetRequest(2, 4, 0),
						new MatchSetRequest(3, 4, 0),
						new MatchSetRequest(4, 4, 0)));

		MatchResponse response = matchRecordingService.recordMatch(request);
		assertThat(response.winningSide()).isEqualTo(MatchSide.A);

		PlayerRanking winnerRanking =
				playerRankingRepository.findByPlayerIdAndMatchType(winner.getId(), MatchType.SINGLES).orElseThrow();
		PlayerRanking loserRanking =
				playerRankingRepository.findByPlayerIdAndMatchType(loser.getId(), MatchType.SINGLES).orElseThrow();

		assertThat(winnerRanking.getWins()).isEqualTo(1);
		assertThat(winnerRanking.getPoints()).isGreaterThan(0);
		assertThat(loserRanking.getLosses()).isEqualTo(1);
		assertThat(loserRanking.getPoints()).isEqualTo(0);

		assertThat(pointTransactionRepository.findByMatchId(response.id())).hasSize(2);
	}

	@Test
	void singlesMatch_closeLoss_awardsLoserProportionalConsolationPoints() {
		Player winner = player("CloseWinner");
		Player loser = player("CloseLoser");

		// loser takes 3 of 7 sets -- a close loss, not a shutout
		MatchRecordRequest request = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(winner.getId())), new MatchTeamRequest(MatchSide.B, List.of(loser.getId()))),
				List.of(
						new MatchSetRequest(1, 4, 1),
						new MatchSetRequest(2, 1, 4),
						new MatchSetRequest(3, 4, 2),
						new MatchSetRequest(4, 2, 4),
						new MatchSetRequest(5, 4, 3),
						new MatchSetRequest(6, 3, 4),
						new MatchSetRequest(7, 4, 1)));

		matchRecordingService.recordMatch(request);

		PlayerRanking loserRanking =
				playerRankingRepository.findByPlayerIdAndMatchType(loser.getId(), MatchType.SINGLES).orElseThrow();
		assertThat(loserRanking.getPoints()).isGreaterThan(0);
	}

	@Test
	void doublesMatch_bothWinningPartnersEarnFullPoints() {
		Player a1 = player("A1");
		Player a2 = player("A2");
		Player b1 = player("B1");
		Player b2 = player("B2");

		MatchRecordRequest request = new MatchRecordRequest(
				MatchType.DOUBLES,
				null,
				List.of(
						new MatchTeamRequest(MatchSide.A, List.of(a1.getId(), a2.getId())),
						new MatchTeamRequest(MatchSide.B, List.of(b1.getId(), b2.getId()))),
				List.of(new MatchSetRequest(1, 4, 1), new MatchSetRequest(2, 4, 2)));

		matchRecordingService.recordMatch(request);

		PlayerRanking a1Ranking = playerRankingRepository.findByPlayerIdAndMatchType(a1.getId(), MatchType.DOUBLES).orElseThrow();
		PlayerRanking a2Ranking = playerRankingRepository.findByPlayerIdAndMatchType(a2.getId(), MatchType.DOUBLES).orElseThrow();

		assertThat(a1Ranking.getPoints()).isEqualTo(a2Ranking.getPoints());
		assertThat(a1Ranking.getPoints()).isGreaterThan(0);
	}

	@Test
	void singlesMatch_withThreePlayersOnOneSide_isRejected() {
		Player p1 = player("P1");
		Player p2 = player("P2");
		Player p3 = player("P3");

		MatchRecordRequest request = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(
						new MatchTeamRequest(MatchSide.A, List.of(p1.getId(), p3.getId())),
						new MatchTeamRequest(MatchSide.B, List.of(p2.getId()))),
				List.of(new MatchSetRequest(1, 4, 0)));

		assertThatThrownBy(() -> matchRecordingService.recordMatch(request)).isInstanceOf(InvalidMatchException.class);

		assertThat(playerRankingRepository.findByPlayerIdAndMatchType(p1.getId(), MatchType.SINGLES)).isEmpty();
	}

	@Test
	void duplicatePlayerAcrossTeams_isRejected() {
		Player p1 = player("P1");
		Player p2 = player("P2");

		MatchRecordRequest request = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(p1.getId())), new MatchTeamRequest(MatchSide.B, List.of(p1.getId()))),
				List.of(new MatchSetRequest(1, 4, 0)));

		assertThatThrownBy(() -> matchRecordingService.recordMatch(request)).isInstanceOf(InvalidMatchException.class);
	}

	@Test
	void tiedSet_isRejected() {
		Player p1 = player("P1");
		Player p2 = player("P2");

		MatchRecordRequest request = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(p1.getId())), new MatchTeamRequest(MatchSide.B, List.of(p2.getId()))),
				List.of(new MatchSetRequest(1, 4, 4)));

		assertThatThrownBy(() -> matchRecordingService.recordMatch(request)).isInstanceOf(InvalidMatchException.class);
	}

	@Test
	void drawnMatch_isRejected() {
		Player p1 = player("P1");
		Player p2 = player("P2");

		MatchRecordRequest request = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(p1.getId())), new MatchTeamRequest(MatchSide.B, List.of(p2.getId()))),
				List.of(new MatchSetRequest(1, 4, 0), new MatchSetRequest(2, 0, 4)));

		assertThatThrownBy(() -> matchRecordingService.recordMatch(request)).isInstanceOf(InvalidMatchException.class);
	}

	@Test
	void repeatedMatches_propagateTierChangesAcrossPlayerPool() {
		Player strong = player("Strong");
		Player weak1 = player("Weak1");
		Player weak2 = player("Weak2");
		Player weak3 = player("Weak3");

		recordSinglesWin(strong, weak1);
		recordSinglesWin(strong, weak2);
		recordSinglesWin(strong, weak3);

		PlayerRanking strongRanking = playerRankingRepository.findByPlayerIdAndMatchType(strong.getId(), MatchType.SINGLES).orElseThrow();
		assertThat(strongRanking.getTier()).isEqualTo(1);
	}

	@Test
	void correctScore_fixesPointsWithoutChangingWinner() {
		Player winner = player("FixWinner");
		Player loser = player("FixLoser");

		MatchResponse original = matchRecordingService.recordMatch(new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(winner.getId())), new MatchTeamRequest(MatchSide.B, List.of(loser.getId()))),
				List.of(new MatchSetRequest(1, 4, 0), new MatchSetRequest(2, 4, 0))));

		// same winner, but a much closer scoreline -- the loser should now earn some consolation points
		matchRecordingService.correctScore(
				original.id(),
				new MatchScoreCorrectionRequest(List.of(new MatchSetRequest(1, 4, 3), new MatchSetRequest(2, 4, 2))));

		PlayerRanking winnerRanking =
				playerRankingRepository.findByPlayerIdAndMatchType(winner.getId(), MatchType.SINGLES).orElseThrow();
		PlayerRanking loserRanking =
				playerRankingRepository.findByPlayerIdAndMatchType(loser.getId(), MatchType.SINGLES).orElseThrow();

		assertThat(winnerRanking.getWins()).isEqualTo(1);
		assertThat(loserRanking.getLosses()).isEqualTo(1);
		assertThat(loserRanking.getPoints()).isGreaterThan(0);
		assertThat(pointTransactionRepository.findByMatchId(original.id())).hasSize(2);
	}

	@Test
	void correctScore_reversedOutcome_flipsWinnerAndRankings() {
		Player playerA = player("FlipA");
		Player playerB = player("FlipB");

		MatchResponse original = matchRecordingService.recordMatch(new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(playerA.getId())), new MatchTeamRequest(MatchSide.B, List.of(playerB.getId()))),
				List.of(new MatchSetRequest(1, 4, 0), new MatchSetRequest(2, 4, 0))));
		assertThat(original.winningSide()).isEqualTo(MatchSide.A);

		// the score was actually the other way around
		MatchResponse corrected = matchRecordingService.correctScore(
				original.id(),
				new MatchScoreCorrectionRequest(List.of(new MatchSetRequest(1, 0, 4), new MatchSetRequest(2, 0, 4))));
		assertThat(corrected.winningSide()).isEqualTo(MatchSide.B);

		PlayerRanking aRanking = playerRankingRepository.findByPlayerIdAndMatchType(playerA.getId(), MatchType.SINGLES).orElseThrow();
		PlayerRanking bRanking = playerRankingRepository.findByPlayerIdAndMatchType(playerB.getId(), MatchType.SINGLES).orElseThrow();

		assertThat(aRanking.getWins()).isZero();
		assertThat(aRanking.getLosses()).isEqualTo(1);
		assertThat(bRanking.getWins()).isEqualTo(1);
		assertThat(bRanking.getLosses()).isZero();
	}

	@Test
	void correctScore_matchNotFound_throws() {
		assertThatThrownBy(() -> matchRecordingService.correctScore(
						999_999L, new MatchScoreCorrectionRequest(List.of(new MatchSetRequest(1, 4, 0)))))
				.isInstanceOf(com.tennisclub.ranking.exception.ResourceNotFoundException.class);
	}

	@Test
	void correctScore_participantWasDeleted_isRejected() {
		Player winner = player("DeletedParticipantWinner");
		Player loser = player("DeletedParticipantLoser");

		MatchResponse original = matchRecordingService.recordMatch(new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(winner.getId())), new MatchTeamRequest(MatchSide.B, List.of(loser.getId()))),
				List.of(new MatchSetRequest(1, 4, 0), new MatchSetRequest(2, 4, 0))));

		playerService.deletePlayer(loser.getId());
		// The delete's DB-level ON DELETE SET NULL isn't visible to already-loaded entities still
		// sitting in this test's shared persistence context (see AbstractIntegrationTest -- the
		// whole test method runs in one transaction); clear it so correctScore does a fresh load,
		// matching what a real second HTTP request would see.
		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> matchRecordingService.correctScore(
						original.id(), new MatchScoreCorrectionRequest(List.of(new MatchSetRequest(1, 4, 3)))))
				.isInstanceOf(InvalidMatchException.class);
	}

	@Test
	void deleteMatch_undoesPointsAndRemovesTheMatch() {
		Player winner = player("DeleteMatchWinner");
		Player loser = player("DeleteMatchLoser");

		MatchResponse recorded = matchRecordingService.recordMatch(new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(winner.getId())), new MatchTeamRequest(MatchSide.B, List.of(loser.getId()))),
				List.of(new MatchSetRequest(1, 4, 2))));

		matchRecordingService.deleteMatch(recorded.id());

		PlayerRanking winnerRanking =
				playerRankingRepository.findByPlayerIdAndMatchType(winner.getId(), MatchType.SINGLES).orElseThrow();
		PlayerRanking loserRanking =
				playerRankingRepository.findByPlayerIdAndMatchType(loser.getId(), MatchType.SINGLES).orElseThrow();

		assertThat(winnerRanking.getPoints()).isZero();
		assertThat(winnerRanking.getWins()).isZero();
		assertThat(loserRanking.getLosses()).isZero();
		assertThat(pointTransactionRepository.findByMatchId(recorded.id())).isEmpty();
	}

	@Test
	void deleteMatch_notFound_throws() {
		assertThatThrownBy(() -> matchRecordingService.deleteMatch(999_999L))
				.isInstanceOf(com.tennisclub.ranking.exception.ResourceNotFoundException.class);
	}

	private void recordSinglesWin(Player winner, Player loser) {
		matchRecordingService.recordMatch(new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(winner.getId())), new MatchTeamRequest(MatchSide.B, List.of(loser.getId()))),
				List.of(new MatchSetRequest(1, 4, 0), new MatchSetRequest(2, 4, 0))));
	}
}
