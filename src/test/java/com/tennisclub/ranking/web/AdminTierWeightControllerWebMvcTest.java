package com.tennisclub.ranking.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennisclub.ranking.config.SecurityConfig;
import com.tennisclub.ranking.security.JwtAuthenticationFilter;
import com.tennisclub.ranking.dto.admin.TierWeightEntryRequest;
import com.tennisclub.ranking.dto.admin.TierWeightEntryResponse;
import com.tennisclub.ranking.dto.admin.TierWeightMatrixResponse;
import com.tennisclub.ranking.dto.admin.TierWeightUpdateRequest;
import com.tennisclub.ranking.service.TierWeightConfigService;
import java.math.BigDecimal;
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
		controllers = AdminTierWeightController.class,
		excludeFilters =
				@ComponentScan.Filter(
						type = FilterType.ASSIGNABLE_TYPE,
						classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AdminTierWeightControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private TierWeightConfigService tierWeightConfigService;

	@Test
	void getMatrix_returnsSixteenEntries() throws Exception {
		List<TierWeightEntryResponse> entries = new java.util.ArrayList<>();
		for (int w = 1; w <= 4; w++) {
			for (int l = 1; l <= 4; l++) {
				entries.add(new TierWeightEntryResponse(w, l, BigDecimal.ONE));
			}
		}
		when(tierWeightConfigService.getMatrix()).thenReturn(new TierWeightMatrixResponse(entries));

		mockMvc.perform(get("/api/admin/tier-weights"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entries.length()").value(16));
	}

	@Test
	void updateMatrix_outOfRangeTier_returns400() throws Exception {
		String invalidJson = "{\"entries\":[{\"winnerTier\":5,\"loserTier\":1,\"weight\":1.5}]}";

		mockMvc.perform(put("/api/admin/tier-weights").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateMatrix_valid_invokesServiceAndReturns200() throws Exception {
		TierWeightUpdateRequest request =
				new TierWeightUpdateRequest(List.of(new TierWeightEntryRequest(4, 1, new BigDecimal("1.60"))));
		when(tierWeightConfigService.updateMatrix(any())).thenReturn(new TierWeightMatrixResponse(List.of()));

		mockMvc.perform(put("/api/admin/tier-weights")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		verify(tierWeightConfigService).updateMatrix(any());
	}
}
