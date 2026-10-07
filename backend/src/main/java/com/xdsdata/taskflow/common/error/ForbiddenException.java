package com.xdsdata.taskflow.common.error;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

	public ForbiddenException() {
		this("You don't have permission to do that.");
	}

	public ForbiddenException(String message) {
		super(HttpStatus.FORBIDDEN, "FORBIDDEN", message, Map.of());
	}

}
