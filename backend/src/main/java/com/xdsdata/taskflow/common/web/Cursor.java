package com.xdsdata.taskflow.common.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.xdsdata.taskflow.common.error.BadRequestException;

/**
 * Opaque keyset-pagination position: the (createdAt, id) of the last item already returned.
 */
public record Cursor(Instant createdAt, UUID id) {

	public String encode() {
		String raw = createdAt.toString() + "|" + id;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	/** @return the decoded cursor, or {@code null} for the first page */
	public static Cursor decode(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
			int separator = raw.indexOf('|');
			return new Cursor(Instant.parse(raw.substring(0, separator)), UUID.fromString(raw.substring(separator + 1)));
		}
		catch (RuntimeException ex) {
			throw new BadRequestException("INVALID_CURSOR", "The page cursor is invalid.");
		}
	}

}
