package com.tennisclub.ranking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Singleton row (id is always 1, seeded by V6__add_season_state.sql) toggling whether new
 * matches can currently be recorded. Lets admins run a "season" as a limited window (e.g. a
 * monthly 2-week focused-training period) without touching the server itself: read endpoints
 * stay up the whole time, only POST /api/matches is gated on this flag.
 */
@Entity
@Table(name = "season_state")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonState {

	public static final short SINGLETON_ID = 1;

	@Id
	private Short id;

	@Column(name = "is_open", nullable = false)
	private boolean open;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
