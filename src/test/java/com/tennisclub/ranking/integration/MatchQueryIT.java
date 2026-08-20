package com.tennisclub.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennisclub.ranking.config.RankingSecurityProperties;
import com.tennisclub.ranking.dto.auth.LoginRequest;
import com.tennisclub.ranking.dto.auth.LoginResponse;
import com.tennisclub.ranking.dto.match.MatchRecordRequest;
import com.tennisclub.ranking.dto.match.MatchSetRequest;
import com.tennisclub.ranking.dto.match.MatchTeamRequest;
import com.tennisclub.ranking.dto.player.PlayerCreateRequest;
import com.tennisclub.ranking.dto.player.PlayerResponse;
import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Deliberately NOT @Transactional: the whole point is to exercise GET /api/matches,
 * GET /api/matches/{id}, and GET /api/leaderboards/{type} the way a real request does — no
 * ambient test transaction propping the Hibernate session open behind the scenes. This is
 * exactly the scenario that let a LazyInitializationException / MultipleBagFetchException slip
 * through earlier (matches list/detail, then again for the leaderboard reading
 * PlayerRanking.player.fullName).
 */
@AutoConfigureMockMvc
class MatchQueryIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RankingSecurityProperties securityProperties;

	@Test
	void listAndDetail_afterRecordingAMatch_returnOkNotServerError() throws Exception {
		String adminToken = loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());

		Long winnerId = createPlayer(adminToken, "match-query-winner");
		Long loserId = createPlayer(adminToken, "match-query-loser");

		MatchRecordRequest matchRequest = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(
						new MatchTeamRequest(MatchSide.A, List.of(winnerId)),
						new MatchTeamRequest(MatchSide.B, List.of(loserId))),
				List.of(new MatchSetRequest(1, 4, 1), new MatchSetRequest(2, 4, 2)));

		String matchResponseBody = mockMvc.perform(post("/api/matches")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(matchRequest)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		long matchId = objectMapper.readTree(matchResponseBody).get("id").asLong();

		String listBody = mockMvc.perform(get("/api/matches").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode listContent = objectMapper.readTree(listBody).get("content");
		assertThat(listContent.isArray()).isTrue();
		assertThat(listContent.size()).isGreaterThanOrEqualTo(1);

		String detailBody = mockMvc.perform(
						get("/api/matches/" + matchId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode detail = objectMapper.readTree(detailBody);
		assertThat(detail.get("teams").size()).isEqualTo(2);
		assertThat(detail.get("sets").size()).isEqualTo(2);
		assertThat(detail.get("pointTransactions").size()).isEqualTo(2);
	}

	@Test
	void singlesLeaderboard_afterRecordingAMatch_returnsOkWithPlayerNames() throws Exception {
		String adminToken = loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());

		Long winnerId = createPlayer(adminToken, "leaderboard-winner");
		Long loserId = createPlayer(adminToken, "leaderboard-loser");

		MatchRecordRequest matchRequest = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(
						new MatchTeamRequest(MatchSide.A, List.of(winnerId)),
						new MatchTeamRequest(MatchSide.B, List.of(loserId))),
				List.of(new MatchSetRequest(1, 4, 0), new MatchSetRequest(2, 4, 1)));

		mockMvc.perform(post("/api/matches")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(matchRequest)))
				.andExpect(status().isCreated());

		String leaderboardBody = mockMvc.perform(
						get("/api/leaderboards/singles").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode leaderboard = objectMapper.readTree(leaderboardBody);
		assertThat(leaderboard.isArray()).isTrue();
		List<String> names = new ArrayList<>();
		leaderboard.forEach(entry -> names.add(entry.get("fullName").asText()));
		assertThat(names).contains("leaderboard-winner", "leaderboard-loser");
	}

	private Long createPlayer(String adminToken, String username) throws Exception {
		PlayerCreateRequest request = new PlayerCreateRequest(username, null, username, "password123", null);
		String body = mockMvc.perform(post("/api/players")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readValue(body, PlayerResponse.class).id();
	}

	private String loginAndGetToken(String username, String password) throws Exception {
		String body = objectMapper.writeValueAsString(new LoginRequest(username, password));
		String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readValue(response, LoginResponse.class).token();
	}
}
