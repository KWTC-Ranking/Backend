package com.tennisclub.ranking.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiError.of(401, "Unauthorized", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiError.of(403, "Forbidden", "You do not have permission to access this resource", request.getRequestURI()));
	}

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

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(ApiError.of(400, "Bad Request", "Request body is missing or malformed", request.getRequestURI()));
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
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of(500, "Internal Server Error", "An unexpected error occurred", request.getRequestURI()));
	}
}
