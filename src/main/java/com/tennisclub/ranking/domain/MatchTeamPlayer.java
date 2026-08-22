package com.tennisclub.ranking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "match_team_player",
		uniqueConstraints = @UniqueConstraint(name = "uq_match_team_player", columnNames = {"match_team_id", "player_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchTeamPlayer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_team_id", nullable = false)
	private MatchTeam matchTeam;

	/**
	 * Nullable: set to NULL (ON DELETE SET NULL, see V5__nullable_player_on_deletion.sql) when the
	 * player is permanently deleted, so the match and everyone else's points survive instead of
	 * being blocked or cascaded away. A null player renders as "(탈퇴한 회원)" in API responses.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id")
	private Player player;

	public MatchTeamPlayer(Player player) {
		this.player = player;
	}
}
