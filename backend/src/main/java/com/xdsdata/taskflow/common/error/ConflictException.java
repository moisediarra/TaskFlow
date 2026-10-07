package com.xdsdata.taskflow.common.error;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

	public ConflictException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message, Map.of());
	}

	/** Conflict attributable to one form field, so the client can show it next to that field. */
	public ConflictException(String code, String message, String field) {
		super(HttpStatus.CONFLICT, code, message, Map.of(field, message));
	}

}
