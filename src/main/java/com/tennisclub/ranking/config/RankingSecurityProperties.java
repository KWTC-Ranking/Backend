package com.tennisclub.ranking.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "ranking.security")
@Validated
public class RankingSecurityProperties {

	@NotBlank
	private String jwtSecret = "dev-only-change-me-please-use-a-long-random-secret-32bytes-min";

	@Positive
	private long jwtExpirationMinutes = 1440;

	@NotBlank
	private String defaultAdminUsername = "admin";

	@NotBlank
	private String defaultAdminPassword = "ChangeMe123!";

	public String getJwtSecret() {
		return jwtSecret;
	}

	public void setJwtSecret(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	public long getJwtExpirationMinutes() {
		return jwtExpirationMinutes;
	}

	public void setJwtExpirationMinutes(long jwtExpirationMinutes) {
		this.jwtExpirationMinutes = jwtExpirationMinutes;
	}

	public String getDefaultAdminUsername() {
		return defaultAdminUsername;
	}

	public void setDefaultAdminUsername(String defaultAdminUsername) {
		this.defaultAdminUsername = defaultAdminUsername;
	}

	public String getDefaultAdminPassword() {
		return defaultAdminPassword;
	}

	public void setDefaultAdminPassword(String defaultAdminPassword) {
		this.defaultAdminPassword = defaultAdminPassword;
	}
}
