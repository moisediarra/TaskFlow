package com.xdsdata.taskflow.search.internal;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.search.internal.SearchService.SearchResults;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SearchController {

	private final SearchService search;

	SearchController(SearchService search) {
		this.search = search;
	}

	@GetMapping("/api/search")
	SearchResults search(@AuthenticationPrincipal AuthUser user, @RequestParam(defaultValue = "") String q) {
		return search.search(user, q);
	}

}
