package com.tennisclub.ranking.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennisclub.ranking.config.SecurityConfig;
import com.tennisclub.ranking.dto.admin.SeasonStateResponse;
import com.tennisclub.ranking.dto.admin.SeasonStateUpdateRequest;
import com.tennisclub.ranking.security.JwtAuthenticationFilter;
import com.tennisclub.ranking.service.SeasonStateService;
import java.time.Instant;
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
		controllers = AdminSeasonController.class,
		excludeFilters =
				@ComponentScan.Filter(
						type = FilterType.ASSIGNABLE_TYPE,
						classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AdminSeasonControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private SeasonStateService seasonStateService;

	@Test
	void updateState_valid_invokesServiceAndReturns200() throws Exception {
		when(seasonStateService.updateState(any())).thenReturn(new SeasonStateResponse(false, Instant.now()));

		mockMvc.perform(put("/api/admin/season")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SeasonStateUpdateRequest(false))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.open").value(false));

		verify(seasonStateService).updateState(any());
	}

	@Test
	void updateState_missingOpenField_returns400() throws Exception {
		mockMvc.perform(put("/api/admin/season").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}
}
