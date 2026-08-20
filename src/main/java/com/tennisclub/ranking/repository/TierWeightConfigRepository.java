package com.tennisclub.ranking.repository;

import com.tennisclub.ranking.domain.TierWeightConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TierWeightConfigRepository extends JpaRepository<TierWeightConfig, Long> {

	Optional<TierWeightConfig> findByWinnerTierAndLoserTier(int winnerTier, int loserTier);

	List<TierWeightConfig> findAllByOrderByWinnerTierAscLoserTierAsc();
}
