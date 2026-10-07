package com.xdsdata.taskflow.common.error;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

	/** @param resource what was not found, e.g. "Project" */
	public NotFoundException(String resource) {
		super(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + " not found.", Map.of());
	}

	public NotFoundException(String code, String message, Map<String, String> fieldErrors) {
		super(HttpStatus.NOT_FOUND, code, message, fieldErrors);
	}

}
