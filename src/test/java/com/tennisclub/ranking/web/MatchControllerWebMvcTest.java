package com.tennisclub.ranking.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennisclub.ranking.config.SecurityConfig;
import com.tennisclub.ranking.security.JwtAuthenticationFilter;
import com.tennisclub.ranking.domain.MatchSide;
import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.dto.match.MatchRecordRequest;
import com.tennisclub.ranking.dto.match.MatchResponse;
import com.tennisclub.ranking.dto.match.MatchScoreCorrectionRequest;
import com.tennisclub.ranking.dto.match.MatchSetRequest;
import com.tennisclub.ranking.dto.match.MatchTeamRequest;
import com.tennisclub.ranking.exception.ResourceNotFoundException;
import com.tennisclub.ranking.repository.MatchRepository;
import com.tennisclub.ranking.repository.PointTransactionRepository;
import com.tennisclub.ranking.service.MatchRecordingService;
import java.util.List;
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
		controllers = MatchController.class,
		excludeFilters =
				@ComponentScan.Filter(
						type = FilterType.ASSIGNABLE_TYPE,
						classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class MatchControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private MatchRecordingService matchRecordingService;

	@MockBean
	private MatchRepository matchRepository;

	@MockBean
	private PointTransactionRepository pointTransactionRepository;

	@Test
	void recordMatch_valid_returns201() throws Exception {
		MatchRecordRequest request = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(1L)), new MatchTeamRequest(MatchSide.B, List.of(2L))),
				List.of(new MatchSetRequest(1, 4, 0)));

		when(matchRecordingService.recordMatch(any()))
				.thenReturn(new MatchResponse(1L, MatchType.SINGLES, null, MatchSide.A, List.of(), List.of(), List.of()));

		mockMvc.perform(post("/api/matches")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());
	}

	@Test
	void recordMatch_missingTeams_returns400() throws Exception {
		String invalidJson = "{\"matchType\":\"SINGLES\",\"sets\":[{\"setNumber\":1,\"teamAGames\":4,\"teamBGames\":0}]}";

		mockMvc.perform(post("/api/matches").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
				.andExpect(status().isBadRequest());
	}

	@Test
	void recordMatch_wrongTeamCount_returns400() throws Exception {
		MatchRecordRequest request = new MatchRecordRequest(
				MatchType.SINGLES,
				null,
				List.of(new MatchTeamRequest(MatchSide.A, List.of(1L))),
				List.of(new MatchSetRequest(1, 4, 0)));

		mockMvc.perform(post("/api/matches")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void correctScore_valid_returns200() throws Exception {
		MatchScoreCorrectionRequest request = new MatchScoreCorrectionRequest(List.of(new MatchSetRequest(1, 4, 3)));

		when(matchRecordingService.correctScore(anyLong(), any()))
				.thenReturn(new MatchResponse(1L, MatchType.SINGLES, null, MatchSide.A, List.of(), List.of(), List.of()));

		mockMvc.perform(put("/api/matches/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
	}

	@Test
	void correctScore_emptySets_returns400() throws Exception {
		mockMvc.perform(put("/api/matches/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sets\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deleteMatch_returns204() throws Exception {
		doNothing().when(matchRecordingService).deleteMatch(1L);

		mockMvc.perform(delete("/api/matches/1")).andExpect(status().isNoContent());
	}

	@Test
	void deleteMatch_notFound_returns404() throws Exception {
		doThrow(new ResourceNotFoundException("Match 99 not found")).when(matchRecordingService).deleteMatch(99L);

		mockMvc.perform(delete("/api/matches/99")).andExpect(status().isNotFound());
	}
}
