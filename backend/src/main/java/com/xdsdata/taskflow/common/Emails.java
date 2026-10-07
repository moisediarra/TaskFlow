package com.xdsdata.taskflow.common;

import java.util.Locale;

public final class Emails {

	private Emails() {
	}

	/** Emails are stored and compared lowercased and trimmed. */
	public static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

}
