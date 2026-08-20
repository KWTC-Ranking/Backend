package com.tennisclub.ranking.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "match_team",
		uniqueConstraints = @UniqueConstraint(name = "uq_match_team_match_side", columnNames = {"match_id", "side"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchTeam {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", nullable = false)
	private Match match;

	@Enumerated(EnumType.STRING)
	@Column(name = "side", nullable = false, length = 1)
	private MatchSide side;

	@Column(name = "sets_won", nullable = false)
	private int setsWon;

	@OneToMany(mappedBy = "matchTeam", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<MatchTeamPlayer> players = new ArrayList<>();

	public MatchTeam(MatchSide side, int setsWon) {
		this.side = side;
		this.setsWon = setsWon;
	}

	public void addPlayer(Player player) {
		MatchTeamPlayer teamPlayer = new MatchTeamPlayer(player);
		players.add(teamPlayer);
		teamPlayer.setMatchTeam(this);
	}
}
