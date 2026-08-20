package com.tennisclub.ranking.repository;

import com.tennisclub.ranking.domain.Player;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

	List<Player> findByActive(boolean active);
}
