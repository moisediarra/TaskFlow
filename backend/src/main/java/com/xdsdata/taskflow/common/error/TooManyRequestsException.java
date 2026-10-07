package com.xdsdata.taskflow.common.error;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {

	public TooManyRequestsException() {
		super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
				"Too many attempts. Please wait a few minutes and try again.", Map.of());
	}

}
