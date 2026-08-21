package com.tennisclub.ranking.repository;

import com.tennisclub.ranking.domain.Player;
import com.tennisclub.ranking.domain.PlayerRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

	List<Player> findByActive(boolean active);

	Optional<Player> findByUsername(String username);

	boolean existsByRole(PlayerRole role);

	List<Player> findByRoleNot(PlayerRole role);
}
