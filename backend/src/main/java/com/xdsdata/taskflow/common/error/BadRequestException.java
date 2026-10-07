package com.xdsdata.taskflow.common.error;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

	public BadRequestException(String code, String message) {
		super(HttpStatus.BAD_REQUEST, code, message, Map.of());
	}

	public BadRequestException(String code, String message, Map<String, String> fieldErrors) {
		super(HttpStatus.BAD_REQUEST, code, message, fieldErrors);
	}

	/** A single invalid form field. */
	public static BadRequestException field(String field, String message) {
		return new BadRequestException("VALIDATION_FAILED", message, Map.of(field, message));
	}

}
