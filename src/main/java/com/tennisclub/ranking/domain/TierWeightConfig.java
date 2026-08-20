package com.tennisclub.ranking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
		name = "tier_weight_config",
		uniqueConstraints = @UniqueConstraint(name = "uq_tier_weight_config_pair", columnNames = {"winner_tier", "loser_tier"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TierWeightConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "winner_tier", nullable = false)
	private int winnerTier;

	@Column(name = "loser_tier", nullable = false)
	private int loserTier;

	@Column(name = "weight", nullable = false, precision = 4, scale = 2)
	private BigDecimal weight;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public TierWeightConfig(int winnerTier, int loserTier, BigDecimal weight) {
		this.winnerTier = winnerTier;
		this.loserTier = loserTier;
		this.weight = weight;
	}
}
