package com.xdsdata.taskflow.common.error;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

	public UnauthorizedException(String code, String message) {
		super(HttpStatus.UNAUTHORIZED, code, message, Map.of());
	}

}
