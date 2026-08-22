package com.tennisclub.ranking.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennisclub.ranking.config.SecurityConfig;
import com.tennisclub.ranking.security.JwtAuthenticationFilter;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRole;
import com.tennisclub.ranking.dto.player.PlayerCreateRequest;
import com.tennisclub.ranking.exception.ResourceNotFoundException;
import com.tennisclub.ranking.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
		controllers = PlayerController.class,
		excludeFilters =
				@ComponentScan.Filter(
						type = FilterType.ASSIGNABLE_TYPE,
						classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class PlayerControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PlayerService playerService;

	private Player samplePlayer(Long id) {
		Player player = new Player("Alice", "alice@example.com", "alice", "hash", PlayerRole.MEMBER);
		player.setId(id);
		return player;
	}

	@Test
	void createPlayer_returns201() throws Exception {
		when(playerService.createPlayer(any())).thenReturn(samplePlayer(1L));

		mockMvc.perform(post("/api/players")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new PlayerCreateRequest("Alice", "alice@example.com", "alice", "password123", null, null))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fullName", is("Alice")));
	}

	@Test
	void createPlayer_blankName_returns400() throws Exception {
		mockMvc.perform(post("/api/players")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new PlayerCreateRequest("", null, "alice", "password123", null, null))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getPlayer_notFound_returns404() throws Exception {
		when(playerService.getPlayer(anyLong())).thenThrow(new ResourceNotFoundException("Player 99 not found"));

		mockMvc.perform(get("/api/players/99")).andExpect(status().isNotFound());
	}

	@Test
	void getPlayer_found_returns200() throws Exception {
		when(playerService.getPlayer(1L)).thenReturn(samplePlayer(1L));

		mockMvc.perform(get("/api/players/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id", is(1)));
	}

	@Test
	void updatePlayer_invalidEmail_returns400() throws Exception {
		mockMvc.perform(put("/api/players/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"not-an-email\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deletePlayer_returns204() throws Exception {
		mockMvc.perform(delete("/api/players/1")).andExpect(status().isNoContent());
	}
}
