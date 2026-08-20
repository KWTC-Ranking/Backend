package com.tennisclub.ranking.repository;

import com.tennisclub.ranking.domain.Match;
import com.tennisclub.ranking.domain.MatchType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, Long> {

	@EntityGraph(attributePaths = {"teams", "teams.players", "teams.players.player", "sets"})
	@Query("select m from Match m where m.id = :id")
	Optional<Match> findByIdWithDetails(@Param("id") Long id);

	@Query(
			"""
			select distinct m from Match m
			left join m.teams t
			left join t.players tp
			where (:matchType is null or m.matchType = :matchType)
			  and (:playerId is null or tp.player.id = :playerId)
			order by m.playedAt desc, m.id desc
			""")
	Page<Match> search(@Param("matchType") MatchType matchType, @Param("playerId") Long playerId, Pageable pageable);
}
