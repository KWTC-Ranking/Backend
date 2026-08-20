package com.tennisclub.ranking.repository;

import com.tennisclub.ranking.domain.Match;
import com.tennisclub.ranking.domain.MatchType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, Long> {

	// No @EntityGraph here on purpose: Match.teams, MatchTeam.players and Match.sets are all
	// unindexed List ("bag") collections, and Hibernate refuses to fetch-join more than one bag
	// in a single query (MultipleBagFetchException) -- true even for a parent/child chain like
	// teams -> teams.players, not just sibling collections. At this data scale (one match, a
	// couple of teams/players/sets) plain lazy loading inside the caller's
	// @Transactional(readOnly = true) scope is simpler and fast enough; it's not worth splitting
	// into multiple queries just to avoid a handful of extra single-row SELECTs.
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
