package com.tennisclub.ranking.exception;

public class PlayerDeletionNotAllowedException extends RuntimeException {

	public PlayerDeletionNotAllowedException(String message) {
		super(message);
	}
}
