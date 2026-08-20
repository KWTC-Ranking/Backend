package com.tennisclub.ranking.service;

import com.tennisclub.ranking.config.RankingSecurityProperties;
import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRole;
import com.tennisclub.ranking.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a single default admin account on first startup so the club owner never has to
 * insert data into the database by hand. Change the password immediately after first login.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountSeeder implements ApplicationRunner {

	private final PlayerRepository playerRepository;
	private final PasswordEncoder passwordEncoder;
	private final RankingSecurityProperties securityProperties;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (playerRepository.existsByRole(PlayerRole.ADMIN)) {
			return;
		}

		Player admin = new Player(
				"Club Admin",
				null,
				securityProperties.getDefaultAdminUsername(),
				passwordEncoder.encode(securityProperties.getDefaultAdminPassword()),
				PlayerRole.ADMIN);
		playerRepository.save(admin);

		log.warn(
				"Seeded default admin account (username='{}'). Log in and change the password immediately, "
						+ "or set ranking.security.default-admin-username/default-admin-password before first startup.",
				securityProperties.getDefaultAdminUsername());
	}
}
