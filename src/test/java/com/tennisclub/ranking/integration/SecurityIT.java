package com.tennisclub.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennisclub.ranking.config.RankingSecurityProperties;
import com.tennisclub.ranking.dto.auth.ChangePasswordRequest;
import com.tennisclub.ranking.dto.auth.LoginRequest;
import com.tennisclub.ranking.dto.auth.LoginResponse;
import com.tennisclub.ranking.dto.player.PasswordResetRequest;
import com.tennisclub.ranking.dto.player.PlayerCreateRequest;
import com.tennisclub.ranking.dto.player.PlayerResponse;
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

	@Test
	void adminChangesOwnPassword_oldPasswordStopsWorking_newPasswordWorks() throws Exception {
		String adminUsername = securityProperties.getDefaultAdminUsername();
		String oldPassword = securityProperties.getDefaultAdminPassword();
		String adminToken = loginAndGetToken(adminUsername, oldPassword);

		String changeBody = objectMapper.writeValueAsString(new ChangePasswordRequest(oldPassword, "new-admin-password-1"));
		mockMvc.perform(post("/api/auth/change-password")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(changeBody))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(adminUsername, oldPassword))))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(adminUsername, "new-admin-password-1"))))
				.andExpect(status().isOk());
	}

	@Test
	void changePassword_wrongCurrentPassword_returns401() throws Exception {
		String adminToken =
				loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());

		String changeBody = objectMapper.writeValueAsString(new ChangePasswordRequest("totally-wrong", "new-admin-password-1"));
		mockMvc.perform(post("/api/auth/change-password")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(changeBody))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminResetsMemberPassword_memberCanLoginWithNewPassword() throws Exception {
		String adminToken =
				loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());

		String createBody = objectMapper.writeValueAsString(
				new PlayerCreateRequest("Forgetful Member", null, "forgetful-member", "original-password", null));
		String createResponse = mockMvc.perform(post("/api/players")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long memberId = objectMapper.readValue(createResponse, PlayerResponse.class).id();

		String resetBody = objectMapper.writeValueAsString(new PasswordResetRequest("reset-by-admin-password"));
		mockMvc.perform(put("/api/players/" + memberId + "/password")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(resetBody))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest("forgetful-member", "original-password"))))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest("forgetful-member", "reset-by-admin-password"))))
				.andExpect(status().isOk());
	}

	@Test
	void nonAdminMember_cannotResetAnotherPlayersPassword() throws Exception {
		String adminToken =
				loginAndGetToken(securityProperties.getDefaultAdminUsername(), securityProperties.getDefaultAdminPassword());

		String createBody =
				objectMapper.writeValueAsString(new PlayerCreateRequest("Plain Member", null, "plain-member", "password123", null));
		mockMvc.perform(post("/api/players")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isCreated());
		String memberToken = loginAndGetToken("plain-member", "password123");

		String resetBody = objectMapper.writeValueAsString(new PasswordResetRequest("hacked-password"));
		mockMvc.perform(put("/api/players/1/password")
						.header("Authorization", "Bearer " + memberToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(resetBody))
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
