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
import com.tennisclub.ranking.dto.player.PlayerCreateRequest;
import com.tennisclub.ranking.dto.player.PlayerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Covers PlayerService.seedRanking — the admin-chosen initial tier at member creation. */
@Transactional
@AutoConfigureMockMvc
class PlayerTierSeedingIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RankingSecurityProperties securityProperties;

	@Test
	void createPlayer_withInitialTierOne_seedsBothDisciplinesAtThatTierWithPositivePoints() throws Exception {
		String adminToken = loginAndGetToken();
		Long playerId = createPlayer(adminToken, "seed-tier-one", 1);

		String rankingsBody = mockMvc.perform(
						get("/api/players/" + playerId + "/rankings").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode rankings = objectMapper.readTree(rankingsBody);

		assertThat(rankings.isArray()).isTrue();
		assertThat(rankings.size()).isEqualTo(2);
		for (JsonNode ranking : rankings) {
			assertThat(ranking.get("tier").asInt()).isEqualTo(1);
			assertThat(ranking.get("points").asInt()).isEqualTo(900); // (4-1) * tier-seed-step(300)
		}
	}

	@Test
	void createPlayer_withInitialTierFour_seedsZeroPointsMatchingOldDefault() throws Exception {
		String adminToken = loginAndGetToken();
		Long playerId = createPlayer(adminToken, "seed-tier-four", 4);

		String rankingsBody = mockMvc.perform(
						get("/api/players/" + playerId + "/rankings").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode rankings = objectMapper.readTree(rankingsBody);

		for (JsonNode ranking : rankings) {
			assertThat(ranking.get("tier").asInt()).isEqualTo(4);
			assertThat(ranking.get("points").asInt()).isZero();
		}
	}

	@Test
	void createPlayer_withoutInitialTier_hasNoRankingsUntilFirstMatch() throws Exception {
		String adminToken = loginAndGetToken();
		Long playerId = createPlayer(adminToken, "seed-tier-none", null);

		String rankingsBody = mockMvc.perform(
						get("/api/players/" + playerId + "/rankings").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode rankings = objectMapper.readTree(rankingsBody);

		assertThat(rankings.isArray()).isTrue();
		assertThat(rankings.size()).isZero();
	}

	private Long createPlayer(String adminToken, String username, Integer initialTier) throws Exception {
		PlayerCreateRequest request = new PlayerCreateRequest(username, null, username, "password123", null, initialTier);
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

	private String loginAndGetToken() throws Exception {
		String body = objectMapper.writeValueAsString(
				new LoginRequest(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword()));
		String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readValue(response, LoginResponse.class).token();
	}
}
