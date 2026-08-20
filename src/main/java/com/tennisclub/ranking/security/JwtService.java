package com.tennisclub.ranking.security;

import com.tennisclub.ranking.config.RankingSecurityProperties;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

	private final RankingSecurityProperties securityProperties;

	public record TokenClaims(Long playerId, String username, PlayerRole role) {}

	public String generateToken(Player player) {
		SecretKey key = key();
		Instant now = Instant.now();
		Instant expiry = now.plus(securityProperties.getJwtExpirationMinutes(), ChronoUnit.MINUTES);

		return Jwts.builder()
				.subject(String.valueOf(player.getId()))
				.claim("username", player.getUsername())
				.claim("role", player.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(key)
				.compact();
	}

	public TokenClaims parse(String token) throws JwtException {
		Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
		Long playerId = Long.valueOf(claims.getSubject());
		String username = claims.get("username", String.class);
		PlayerRole role = PlayerRole.valueOf(claims.get("role", String.class));
		return new TokenClaims(playerId, username, role);
	}

	private SecretKey key() {
		return Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
	}
}
