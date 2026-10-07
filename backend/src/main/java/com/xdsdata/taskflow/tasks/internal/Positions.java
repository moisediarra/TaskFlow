package com.xdsdata.taskflow.tasks.internal;

/**
 * Card ordering inside a column. Positions are spaced by {@link #GAP}; a card dropped between two others
 * takes the midpoint, so a move updates a single row. When repeated insertions exhaust the space between
 * two neighbours the column is renumbered.
 */
final class Positions {

	static final double GAP = 1024;

	/** Below this distance between neighbours the midpoint loses precision, so renumber first. */
	static final double MIN_DISTANCE = 1e-6;

	private Positions() {
	}

	/**
	 * Position for a card placed directly below {@code above} and directly above {@code below}; either
	 * neighbour may be null (top of column, bottom of column, or empty column).
	 */
	static double between(Double above, Double below) {
		if (above == null && below == null) {
			return GAP;
		}
		if (above == null) {
			return below - GAP;
		}
		if (below == null) {
			return above + GAP;
		}
		return (above + below) / 2;
	}

	/** True when the neighbours are too close (or out of order) to fit a card between them. */
	static boolean needsRenumbering(Double above, Double below) {
		return above != null && below != null && below - above < MIN_DISTANCE;
	}

}
