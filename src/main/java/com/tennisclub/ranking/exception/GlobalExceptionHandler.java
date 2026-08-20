package com.tennisclub.ranking.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiError.of(404, "Not Found", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(InvalidMatchException.class)
	public ResponseEntity<ApiError> handleInvalidMatch(InvalidMatchException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(ApiError.of(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(ApiError.of(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> details = new HashMap<>();
		ex.getBindingResult()
				.getFieldErrors()
				.forEach(fieldError -> details.put(fieldError.getField(), fieldError.getDefaultMessage()));
		return ResponseEntity.badRequest()
				.body(ApiError.of(400, "Bad Request", "Validation failed", request.getRequestURI(), details));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of(500, "Internal Server Error", "An unexpected error occurred", request.getRequestURI()));
	}
}
