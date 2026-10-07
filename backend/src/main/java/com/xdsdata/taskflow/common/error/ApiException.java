package com.xdsdata.taskflow.common.error;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected business errors. The message is shown to users, so it must be friendly and
 * must never contain technical details.
 */
public abstract class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private final Map<String, String> fieldErrors;

	protected ApiException(HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
		super(message);
		this.status = status;
		this.code = code;
		this.fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public Map<String, String> fieldErrors() {
		return fieldErrors;
	}

}
