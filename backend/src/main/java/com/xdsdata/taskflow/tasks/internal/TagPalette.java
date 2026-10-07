package com.xdsdata.taskflow.tasks.internal;

import java.util.List;
import java.util.Locale;

/** Tag colors are chosen from a fixed palette by name, so the same tag name always gets the same color. */
final class TagPalette {

	static final List<String> COLORS = List.of("teal", "sky", "indigo", "violet", "fuchsia", "rose", "orange",
			"amber", "lime", "emerald", "slate");

	private TagPalette() {
	}

	static String colorFor(String tagName) {
		int index = Math.floorMod(tagName.trim().toLowerCase(Locale.ROOT).hashCode(), COLORS.size());
		return COLORS.get(index);
	}

}
