package com.tennisclub.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennisclub.ranking.config.RankingSecurityProperties;
import com.tennisclub.ranking.dto.auth.LoginRequest;
import com.tennisclub.ranking.dto.auth.LoginResponse;
import com.tennisclub.ranking.dto.player.PlayerCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@AutoConfigureMockMvc
class SecurityIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RankingSecurityProperties securityProperties;

	@Test
	void protectedEndpoint_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/leaderboards/singles")).andExpect(status().isUnauthorized());
	}

	@Test
	void loginWithSeededAdmin_thenAccessProtectedEndpoint_succeeds() throws Exception {
		String token = loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());

		mockMvc.perform(get("/api/leaderboards/singles").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void loginWithWrongPassword_returns401() throws Exception {
		String body = objectMapper.writeValueAsString(new LoginRequest(securityProperties.getDefaultAdminUsername(), "wrong-password"));

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void nonAdminMember_cannotCreatePlayers() throws Exception {
		String adminToken = loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());

		String createMemberBody = objectMapper.writeValueAsString(
				new PlayerCreateRequest("Regular Member", null, "regular-member", "password123", null));
		mockMvc.perform(post("/api/players")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createMemberBody))
				.andExpect(status().isCreated());

		String memberToken = loginAndGetToken("regular-member", "password123");

		String createAnotherBody =
				objectMapper.writeValueAsString(new PlayerCreateRequest("Someone Else", null, "someone-else", "password123", null));
		mockMvc.perform(post("/api/players")
						.header("Authorization", "Bearer " + memberToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createAnotherBody))
				.andExpect(status().isForbidden());
	}

	private String loginAndGetToken(String username, String password) throws Exception {
		String body = objectMapper.writeValueAsString(new LoginRequest(username, password));
		String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
		assertThat(loginResponse.token()).isNotBlank();
		return loginResponse.token();
	}
}
