package com.tennisclub.ranking.repository;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.PlayerRanking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRankingRepository extends JpaRepository<PlayerRanking, Long> {

	Optional<PlayerRanking> findByPlayerIdAndMatchType(Long playerId, MatchType matchType);

	List<PlayerRanking> findByMatchTypeOrderByPointsDescIdAsc(MatchType matchType);

	List<PlayerRanking> findByPlayerId(Long playerId);
}
