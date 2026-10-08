package com.xdsdata.taskflow.common;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.common.web.Cursor;
import com.xdsdata.taskflow.common.web.SearchText;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonTests {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);

	@Test
	void dueStateFollowsClaudeMdSection18() {
		assertThat(DueState.of(null, false, TODAY)).isEqualTo(DueState.NONE);
		assertThat(DueState.of(TODAY.plusDays(8), false, TODAY)).isEqualTo(DueState.UPCOMING);
		assertThat(DueState.of(TODAY, false, TODAY)).isEqualTo(DueState.DUE_TODAY);
		assertThat(DueState.of(TODAY.minusDays(1), false, TODAY)).isEqualTo(DueState.OVERDUE);
		assertThat(DueState.of(TODAY.minusDays(1), true, TODAY)).isEqualTo(DueState.COMPLETED);
	}

	@Test
	void cursorsRoundTripAndRejectGarbage() {
		Cursor cursor = new Cursor(Instant.parse("2026-10-07T10:42:00.123456Z"), UUID.randomUUID());
		assertThat(Cursor.decode(cursor.encode())).isEqualTo(cursor);
		assertThat(Cursor.decode(null)).isNull();
		assertThatThrownBy(() -> Cursor.decode("not-a-cursor")).isInstanceOf(BadRequestException.class);
	}

	@Test
	void searchPatternsEscapeWildcards() {
		assertThat(SearchText.containsPattern("  50%_off  ")).isEqualTo("%50\\%\\_off%");
		assertThat(SearchText.normalize("a   b")).isEqualTo("a b");
		assertThat(SearchText.normalize("x".repeat(500))).hasSize(SearchText.MAX_QUERY_LENGTH);
	}

	@Test
	void emailsAreNormalized() {
		assertThat(Emails.normalize("  John.Doe@Example.COM ")).isEqualTo("john.doe@example.com");
	}

}
