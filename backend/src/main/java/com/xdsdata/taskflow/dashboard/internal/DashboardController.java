package com.xdsdata.taskflow.dashboard.internal;

import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.dashboard.internal.DashboardDtos.DashboardDto;
import com.xdsdata.taskflow.dashboard.internal.DashboardDtos.ProjectOverviewDto;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DashboardController {

	private final DashboardService dashboards;

	DashboardController(DashboardService dashboards) {
		this.dashboards = dashboards;
	}

	@GetMapping("/api/dashboard")
	DashboardDto dashboard(@AuthenticationPrincipal AuthUser user) {
		return dashboards.dashboard(user);
	}

	/** Project monitoring: members and IT Managers. */
	@GetMapping("/api/projects/{projectId}/overview")
	ProjectOverviewDto overview(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId) {
		return dashboards.overview(user, projectId);
	}

}
