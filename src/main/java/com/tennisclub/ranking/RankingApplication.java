package com.tennisclub.ranking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Excludes UserDetailsServiceAutoConfiguration: auth is handled entirely by AuthController +
 * JwtAuthenticationFilter against the Player table, so Spring Boot's default generated-password
 * in-memory user would otherwise be created (and logged) for no reason.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class RankingApplication {

	public static void main(String[] args) {
		SpringApplication.run(RankingApplication.class, args);
	}
}
