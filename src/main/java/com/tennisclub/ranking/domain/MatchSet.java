package com.tennisclub.ranking.domain;

import jakarta.persistence.Column;
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
		name = "match_set",
		uniqueConstraints = @UniqueConstraint(name = "uq_match_set_match_number", columnNames = {"match_id", "set_number"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchSet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", nullable = false)
	private Match match;

	@Column(name = "set_number", nullable = false)
	private int setNumber;

	@Column(name = "team_a_games", nullable = false)
	private int teamAGames;

	@Column(name = "team_b_games", nullable = false)
	private int teamBGames;

	public MatchSet(int setNumber, int teamAGames, int teamBGames) {
		this.setNumber = setNumber;
		this.teamAGames = teamAGames;
		this.teamBGames = teamBGames;
	}
}
