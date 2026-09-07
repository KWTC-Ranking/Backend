package com.tennisclub.ranking.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tennisclub.ranking.config.SecurityConfig;
import com.tennisclub.ranking.dto.admin.SeasonStateResponse;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
		controllers = SeasonController.class,
		excludeFilters =
				@ComponentScan.Filter(
						type = FilterType.ASSIGNABLE_TYPE,
						classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class SeasonControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private SeasonStateService seasonStateService;

	@Test
	void getState_returnsCurrentFlag() throws Exception {
		when(seasonStateService.getState()).thenReturn(new SeasonStateResponse(false, Instant.now()));

		mockMvc.perform(get("/api/season"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.open").value(false));
	}
}
