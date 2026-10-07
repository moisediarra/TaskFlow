package com.xdsdata.taskflow.common.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Page-numbered list envelope used by every paginated table endpoint. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

	public static <T, R> PageResponse<R> of(Page<T> page, Function<T, R> mapper) {
		return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

}
