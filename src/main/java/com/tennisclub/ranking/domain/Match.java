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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "match")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_type", nullable = false, length = 20)
	private MatchType matchType;

	@Column(name = "played_at", nullable = false)
	private Instant playedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "winning_side", nullable = false, length = 1)
	private MatchSide winningSide;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<MatchTeam> teams = new ArrayList<>();

	@OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("setNumber ASC")
	private List<MatchSet> sets = new ArrayList<>();

	public Match(MatchType matchType, Instant playedAt) {
		this.matchType = matchType;
		this.playedAt = playedAt;
	}

	public void addTeam(MatchTeam team) {
		teams.add(team);
		team.setMatch(this);
	}

	public void addSet(MatchSet set) {
		sets.add(set);
		set.setMatch(this);
	}
}
