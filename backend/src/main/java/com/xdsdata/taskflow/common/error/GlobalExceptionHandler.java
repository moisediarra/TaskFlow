package com.xdsdata.taskflow.common.error;

import java.util.LinkedHashMap;
import java.util.Map;

import com.xdsdata.taskflow.common.web.RequestIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 problem document with a stable {@code code}, optional
 * {@code fieldErrors} and the request id. Unexpected errors are logged and never exposed (claude.md §37).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
		return problem(ex.status(), ex.code(), ex.getMessage(), ex.fieldErrors());
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Please sign in to continue.", Map.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
		return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "You don't have permission to do that.", Map.of());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getConstraintViolations().forEach(violation -> {
			String path = violation.getPropertyPath().toString();
			String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
			errors.putIfAbsent(field, violation.getMessage());
		});
		return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Please correct the highlighted fields.", errors);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return problem(HttpStatus.CONFLICT, "CONFLICT",
				"This change conflicts with existing data. Refresh the page and try again.", Map.of());
	}

	@ExceptionHandler({ OptimisticLockingFailureException.class, PessimisticLockingFailureException.class })
	ResponseEntity<ProblemDetail> handleConcurrentUpdate(RuntimeException ex) {
		log.info("Concurrent update rejected: {}", ex.getMessage());
		return problem(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
				"Someone else changed this at the same time. Refresh the page and try again.", Map.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
		log.error("Unexpected error (requestId={})", MDC.get(RequestIdFilter.MDC_KEY), ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
				"Something went wrong on our side. Please try again.", Map.of());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.isBindingFailure() ? "Invalid value." : error.getDefaultMessage());
		}
		ex.getBindingResult().getGlobalErrors()
			.forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
		return widen(problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Please correct the highlighted fields.", errors));
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getParameterValidationResults()
			.forEach(result -> result.getResolvableErrors()
				.forEach(error -> errors.putIfAbsent(result.getMethodParameter().getParameterName(),
						error.getDefaultMessage())));
		return widen(problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Please correct the highlighted fields.", errors));
	}

	/** Standard Spring MVC errors (malformed body, unknown route, wrong method...) get the same shape. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusCode, friendlyMessage(statusCode));
		decorate(problem, codeFor(statusCode), Map.of());
		return super.handleExceptionInternal(ex, problem, headers, statusCode, request);
	}

	public static ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String code, String message,
			Map<String, String> fieldErrors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
		decorate(problem, code, fieldErrors);
		return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
	}

	private static void decorate(ProblemDetail problem, String code, Map<String, String> fieldErrors) {
		HttpStatus resolved = HttpStatus.resolve(problem.getStatus());
		if (resolved != null) {
			problem.setTitle(resolved.getReasonPhrase());
		}
		problem.setProperty("code", code);
		String requestId = MDC.get(RequestIdFilter.MDC_KEY);
		if (requestId != null) {
			problem.setProperty("requestId", requestId);
		}
		if (!fieldErrors.isEmpty()) {
			problem.setProperty("fieldErrors", fieldErrors);
		}
	}

	private static String codeFor(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> "BAD_REQUEST";
			case 401 -> "UNAUTHENTICATED";
			case 403 -> "FORBIDDEN";
			case 404 -> "NOT_FOUND";
			case 405 -> "METHOD_NOT_ALLOWED";
			case 406 -> "NOT_ACCEPTABLE";
			case 413 -> "PAYLOAD_TOO_LARGE";
			case 415 -> "UNSUPPORTED_MEDIA_TYPE";
			default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "ERROR";
		};
	}

	private static String friendlyMessage(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> "The request is invalid.";
			case 404 -> "The requested resource was not found.";
			case 405 -> "This operation is not supported.";
			case 413 -> "The request is too large.";
			case 415 -> "Unsupported content type.";
			default -> status.is5xxServerError() ? "Something went wrong on our side. Please try again."
					: "The request could not be processed.";
		};
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ResponseEntity<Object> widen(ResponseEntity<ProblemDetail> response) {
		return (ResponseEntity) response;
	}

}
