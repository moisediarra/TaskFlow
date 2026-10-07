package com.xdsdata.taskflow.activity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Activity entry for feeds and logs. Names come from the snapshots taken when the action happened.
 */
public record ActivityDto(UUID id, ActivityAction action, String description, UUID actorId, String actorName,
		UUID projectId, String projectName, UUID taskId, String taskTitle, Map<String, Object> metadata,
		Instant createdAt) {

}
