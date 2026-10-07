package com.xdsdata.taskflow.activity.internal;

import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityDto;
import com.xdsdata.taskflow.activity.ActivityService;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.web.CursorPage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ActivityController {

	private final ActivityService activity;

	ActivityController(ActivityService activity) {
		this.activity = activity;
	}

	@GetMapping("/api/projects/{projectId}/activity")
	CursorPage<ActivityDto> projectActivity(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		return activity.projectFeed(user, projectId, cursor, limit);
	}

}
