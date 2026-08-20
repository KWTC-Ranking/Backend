package com.tennisclub.ranking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI rankingOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Tennis Club Ranking API")
						.description("Singles/doubles ranking, match recording, and tier-weighted scoring for the club.")
						.version("v1"));
	}
}
