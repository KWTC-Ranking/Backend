package com.tennisclub.ranking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "point_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Nullable: set to NULL when the player is permanently deleted (see MatchTeamPlayer.player). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id")
	private Player player;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", nullable = false)
	private Match match;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_type", nullable = false, length = 20)
	private MatchType matchType;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 10)
	private MatchOutcome role;

	@Column(name = "base_points", nullable = false)
	private int basePoints;

	@Column(name = "tier_weight", nullable = false, precision = 4, scale = 2)
	private BigDecimal tierWeight;

	@Column(name = "margin_weight", nullable = false, precision = 4, scale = 2)
	private BigDecimal marginWeight;

	@Column(name = "winner_tier_at_match", nullable = false)
	private int winnerTierAtMatch;

	@Column(name = "loser_tier_at_match", nullable = false)
	private int loserTierAtMatch;

	@Column(name = "game_margin", nullable = false)
	private int gameMargin;

	@Column(name = "points_before", nullable = false)
	private int pointsBefore;

	@Column(name = "points_after", nullable = false)
	private int pointsAfter;

	@Column(name = "points_awarded", nullable = false)
	private int pointsAwarded;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Builder
	public PointTransaction(
			Player player,
			Match match,
			MatchType matchType,
			MatchOutcome role,
			int basePoints,
			BigDecimal tierWeight,
			BigDecimal marginWeight,
			int winnerTierAtMatch,
			int loserTierAtMatch,
			int gameMargin,
			int pointsBefore,
			int pointsAfter,
			int pointsAwarded) {
		this.player = player;
		this.match = match;
		this.matchType = matchType;
		this.role = role;
		this.basePoints = basePoints;
		this.tierWeight = tierWeight;
		this.marginWeight = marginWeight;
		this.winnerTierAtMatch = winnerTierAtMatch;
		this.loserTierAtMatch = loserTierAtMatch;
		this.gameMargin = gameMargin;
		this.pointsBefore = pointsBefore;
		this.pointsAfter = pointsAfter;
		this.pointsAwarded = pointsAwarded;
	}
}
