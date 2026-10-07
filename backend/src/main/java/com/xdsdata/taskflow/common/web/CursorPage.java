package com.xdsdata.taskflow.common.web;

import java.util.List;

/**
 * Cursor-paginated list ("load more") for feeds that grow without bound, such as activity logs and
 * notifications. {@code nextCursor} is null on the last page.
 */
public record CursorPage<T>(List<T> items, String nextCursor) {

}
