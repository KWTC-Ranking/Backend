package com.tennisclub.ranking.repository;

import com.tennisclub.ranking.domain.SeasonState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonStateRepository extends JpaRepository<SeasonState, Short> {}
