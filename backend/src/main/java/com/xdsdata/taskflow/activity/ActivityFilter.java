package com.xdsdata.taskflow.activity;

import java.time.Instant;
import java.util.UUID;

/** Optional filters for the global activity log; null means "any". */
public record ActivityFilter(UUID userId, UUID projectId, ActivityAction action, Instant from, Instant to,
		String query) {

	public static ActivityFilter none() {
		return new ActivityFilter(null, null, null, null, null, null);
	}

}
