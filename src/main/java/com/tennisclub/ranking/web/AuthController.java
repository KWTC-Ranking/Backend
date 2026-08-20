package com.tennisclub.ranking.web;

import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.dto.auth.ChangePasswordRequest;
import com.tennisclub.ranking.dto.auth.LoginRequest;
import com.tennisclub.ranking.dto.auth.LoginResponse;
import com.tennisclub.ranking.repository.PlayerRepository;
import com.tennisclub.ranking.security.JwtService;
import com.tennisclub.ranking.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final PlayerRepository playerRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final PlayerService playerService;

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Player player = playerRepository
				.findByUsername(request.username())
				.filter(Player::isActive)
				.orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

		if (!passwordEncoder.matches(request.password(), player.getPasswordHash())) {
			throw new BadCredentialsException("Invalid username or password");
		}

		String token = jwtService.generateToken(player);
		return new LoginResponse(token, player.getId(), player.getUsername(), player.getRole());
	}

	/** Any logged-in player (including admin) changes their own password. Requires the current one. */
	@PostMapping("/change-password")
	public ResponseEntity<Void> changePassword(
			@AuthenticationPrincipal Long playerId, @Valid @RequestBody ChangePasswordRequest request) {
		playerService.changePassword(playerId, request.currentPassword(), request.newPassword());
		return ResponseEntity.noContent().build();
	}
}
