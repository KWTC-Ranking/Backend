package com.tennisclub.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennisclub.ranking.config.RankingSecurityProperties;
import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.PlayerRole;
import com.tennisclub.ranking.dto.auth.LoginRequest;
import com.tennisclub.ranking.dto.auth.LoginResponse;
import com.tennisclub.ranking.dto.match.MatchRecordRequest;
import com.tennisclub.ranking.dto.match.MatchSetRequest;
import com.tennisclub.ranking.dto.match.MatchTeamRequest;
import com.tennisclub.ranking.dto.player.PlayerCreateRequest;
import com.tennisclub.ranking.dto.player.PlayerResponse;
import com.tennisclub.ranking.dto.player.PlayerUpdateRequest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Transactional so the full-DB wipe in resetTestData_deletesEverythingExceptAdmin rolls back at
 * the end of the test instead of permanently clearing out the shared Testcontainers Postgres that
 * every other IT class in this run reuses (see AbstractIntegrationTest — POSTGRES is one static
 * container for the whole suite).
 */
@Transactional
@AutoConfigureMockMvc
class PlayerDeletionIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RankingSecurityProperties securityProperties;

	@Autowired
	private EntityManager entityManager;

	@Test
	void deletePlayer_withNoMatchHistory_removesThemAndReturns404OnLookup() throws Exception {
		String adminToken = loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());
		Long playerId = createPlayer(adminToken, "delete-me-no-history");

		mockMvc.perform(delete("/api/players/" + playerId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/players/" + playerId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletePlayer_withMatchHistory_removesThemButPreservesTheMatchAndOpponentRanking() throws Exception {
		String adminToken = loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());
		Long winnerId = createPlayer(adminToken, "history-winner");
		Long loserId = createPlayer(adminToken, "history-loser");
		Long matchId = recordSinglesMatch(adminToken, winnerId, loserId);

		mockMvc.perform(delete("/api/players/" + winnerId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());

		// The delete's DB-level ON DELETE SET NULL isn't visible to already-loaded entities still
		// sitting in this test's shared persistence context (see the class Javadoc -- the whole test
		// method runs in one transaction); clear it so the reads below see what a real second HTTP
		// request would see.
		entityManager.flush();
		entityManager.clear();

		mockMvc.perform(get("/api/players/" + winnerId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound());

		// the match itself, and the surviving loser's own ranking, must still be intact
		mockMvc.perform(get("/api/matches/" + matchId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teams[0].players[0].fullName", is("(탈퇴한 회원)")));

		mockMvc.perform(get("/api/players/" + loserId + "/rankings").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].losses", is(1)));
	}

	@Test
	void updatePlayer_withRole_promotesMemberToAdmin() throws Exception {
		String adminToken = loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());
		Long memberId = createPlayer(adminToken, "promote-me");

		PlayerUpdateRequest request = new PlayerUpdateRequest(null, null, null, PlayerRole.ADMIN);
		mockMvc.perform(put("/api/players/" + memberId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role", is("ADMIN")));
	}

	@Test
	void resetTestData_deletesEverythingExceptAdmin() throws Exception {
		String adminToken = loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());
		Long winnerId = createPlayer(adminToken, "reset-winner");
		Long loserId = createPlayer(adminToken, "reset-loser");
		recordSinglesMatch(adminToken, winnerId, loserId);

		// >= rather than exact: other IT classes in this suite share the same static Testcontainers
		// Postgres (see AbstractIntegrationTest) and aren't all @Transactional, so they can leave
		// their own non-admin players/matches committed before this test runs.
		mockMvc.perform(post("/api/admin/data/reset-test-data").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deletedPlayers", greaterThanOrEqualTo(2)))
				.andExpect(jsonPath("$.deletedMatches", greaterThanOrEqualTo(1)));

		mockMvc.perform(get("/api/players/" + winnerId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound());

		String matchListBody = mockMvc.perform(get("/api/matches").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode content = objectMapper.readTree(matchListBody).get("content");
		assertThat(content.size()).isZero();

		// Admin itself must survive the reset so whoever ran it doesn't get locked out.
		mockMvc.perform(get("/api/leaderboards/singles").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
	}

	private Long recordSinglesMatch(String adminToken, Long winnerId, Long loserId) throws Exception {
		MatchRecordRequest matchRequest = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(
						new MatchTeamRequest(MatchSide.A, List.of(winnerId)),
						new MatchTeamRequest(MatchSide.B, List.of(loserId))),
				List.of(new MatchSetRequest(1, 4, 1), new MatchSetRequest(2, 4, 2)));

		String body = mockMvc.perform(post("/api/matches")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(matchRequest)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(body).get("id").asLong();
	}

	private Long createPlayer(String adminToken, String username) throws Exception {
		PlayerCreateRequest request = new PlayerCreateRequest(username, null, username, "password123", null, null);
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
