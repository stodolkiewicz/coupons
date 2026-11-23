package com.empik.coupons.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(CouponCodeAlreadyExistsException.class)
	public ProblemDetail handleCouponCodeAlreadyExists(
			CouponCodeAlreadyExistsException exception,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		problem.setTitle("Coupon code already exists");
		problem.setDetail(exception.getMessage());
		problem.setProperty("couponCode", exception.getCode());
		addRequestDetails(problem, request);
		return problem;
	}

	@ExceptionHandler(CouponNotFoundException.class)
	public ProblemDetail handleCouponNotFound(
			CouponNotFoundException exception,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("Coupon not found");
		problem.setDetail(exception.getMessage());
		problem.setProperty("couponCode", exception.getCode());
		addRequestDetails(problem, request);
		return problem;
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ProblemDetail handleUserNotFound(
			UserNotFoundException exception,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("User not found");
		problem.setDetail(exception.getMessage());
		problem.setProperty("userId", exception.getUserId());
		addRequestDetails(problem, request);
		return problem;
	}

	@ExceptionHandler(ClientCountryUnknownException.class)
	public ProblemDetail handleClientCountryUnknown(
			ClientCountryUnknownException exception,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		problem.setTitle("Country cannot be determined");
		problem.setDetail(exception.getMessage());
		addRequestDetails(problem, request);
		return problem;
	}

	@ExceptionHandler(CouponCountryNotAllowedException.class)
	public ProblemDetail handleCouponCountryNotAllowed(
			CouponCountryNotAllowedException exception,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		problem.setTitle("Country not allowed");
		problem.setDetail(exception.getMessage());
		problem.setProperty("couponCode", exception.getCode());
		problem.setProperty("couponCountryCode", exception.getCouponCountryCode());
		problem.setProperty("clientCountryCode", exception.getClientCountryCode());
		addRequestDetails(problem, request);
		return problem;
	}

	@ExceptionHandler(CouponAlreadyUsedByUserException.class)
	public ProblemDetail handleCouponAlreadyUsedByUser(
			CouponAlreadyUsedByUserException exception,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		problem.setTitle("Coupon already used by user");
		problem.setDetail(exception.getMessage());
		problem.setProperty("couponCode", exception.getCode());
		problem.setProperty("userId", exception.getUserId());
		addRequestDetails(problem, request);
		return problem;
	}

	@ExceptionHandler(CouponExhaustedException.class)
	public ProblemDetail handleCouponExhausted(
			CouponExhaustedException exception,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		problem.setTitle("Coupon exhausted");
		problem.setDetail(exception.getMessage());
		problem.setProperty("couponCode", exception.getCode());
		addRequestDetails(problem, request);
		return problem;
	}

	/**
	 * The DB-IP Lite file cannot be read, so the fault is on the server side.
	 * The response hides the exception message, because it names the internal file.
	 */
	@ExceptionHandler(GeoIpLookupException.class)
	public ProblemDetail handleGeoIpLookup(
			GeoIpLookupException exception,
			HttpServletRequest request
	) {
		log.error(
				"Country lookup failed on {} {}",
				request.getMethod(),
				request.getRequestURI(),
				exception
		);
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setTitle("Country lookup failed");
		problem.setDetail("The country of the IP address cannot be determined.");
		addRequestDetails(problem, request);
		return problem;
	}

	/**
	 * Handles every exception that no other handler matches.
	 * The response hides the exception message, because it can contain internal details.
	 */
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(
			Exception exception,
			HttpServletRequest request
	) {
		log.error(
				"Unexpected error on {} {}",
				request.getMethod(),
				request.getRequestURI(),
				exception
		);
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setTitle("Internal server error");
		problem.setDetail("An unexpected error occurred.");
		addRequestDetails(problem, request);
		return problem;
	}

	/**
	 * Adds the list of invalid fields to the default 400 response.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		List<FieldViolation> violations = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> new FieldViolation(
						error.getField(),
						error.getDefaultMessage()
				))
				.toList();
		ProblemDetail problem = exception.getBody();
		problem.setProperty("errors", violations);
		return handleExceptionInternal(
				exception,
				problem,
				headers,
				status,
				request
		);
	}

	/**
	 * The parent class builds every response in this method,
	 * so the override adds the request details to all standard Spring MVC errors.
	 */
	@Override
	protected ResponseEntity<Object> createResponseEntity(
			Object body,
			HttpHeaders headers,
			HttpStatusCode statusCode,
			WebRequest request
	) {
		if (body instanceof ProblemDetail problem && request instanceof ServletWebRequest servletRequest) {
			addRequestDetails(problem, servletRequest.getRequest());
		}
		return super.createResponseEntity(
				body,
				headers,
				statusCode,
				request
		);
	}

	private static void addRequestDetails(
			ProblemDetail problem,
			HttpServletRequest request
	) {
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("timestamp", Instant.now());
	}

	private record FieldViolation(
			String field,
			String message
	) {
	}

}
