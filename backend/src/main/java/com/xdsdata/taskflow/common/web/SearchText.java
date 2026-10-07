package com.xdsdata.taskflow.common.web;

/**
 * Helpers for case-insensitive "contains" searches backed by trigram indexes.
 */
public final class SearchText {

	public static final int MAX_QUERY_LENGTH = 100;

	private SearchText() {
	}

	/** Trims, collapses whitespace and caps the length; returns an empty string for null. */
	public static String normalize(String query) {
		if (query == null) {
			return "";
		}
		String normalized = query.trim().replaceAll("\\s+", " ");
		return normalized.length() > MAX_QUERY_LENGTH ? normalized.substring(0, MAX_QUERY_LENGTH) : normalized;
	}

	/** LIKE pattern matching the text anywhere, with LIKE wildcards in the user's input escaped. */
	public static String containsPattern(String query) {
		String escaped = normalize(query).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
