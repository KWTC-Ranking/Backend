package com.tennisclub.ranking.repository;

import com.tennisclub.ranking.domain.MatchType;
import com.tennisclub.ranking.domain.PointTransaction;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

	Page<PointTransaction> findByPlayerIdOrderByCreatedAtDesc(Long playerId, Pageable pageable);

	Page<PointTransaction> findByPlayerIdAndMatchTypeOrderByCreatedAtDesc(
			Long playerId, MatchType matchType, Pageable pageable);

	List<PointTransaction> findByMatchId(Long matchId);
}
