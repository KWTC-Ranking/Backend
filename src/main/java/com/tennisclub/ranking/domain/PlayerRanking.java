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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
		name = "player_ranking",
		uniqueConstraints = @UniqueConstraint(name = "uq_player_ranking_player_match_type", columnNames = {"player_id", "match_type"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerRanking {

	public static final int INITIAL_TIER = 4;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_type", nullable = false, length = 20)
	private MatchType matchType;

	@Column(name = "points", nullable = false)
	private int points = 0;

	@Column(name = "tier", nullable = false)
	private int tier = INITIAL_TIER;

	@Column(name = "wins", nullable = false)
	private int wins = 0;

	@Column(name = "losses", nullable = false)
	private int losses = 0;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public PlayerRanking(Player player, MatchType matchType) {
		this.player = player;
		this.matchType = matchType;
		this.points = 0;
		this.tier = INITIAL_TIER;
		this.wins = 0;
		this.losses = 0;
	}
}
