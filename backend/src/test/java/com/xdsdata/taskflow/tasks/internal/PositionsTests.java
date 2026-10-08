package com.xdsdata.taskflow.tasks.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PositionsTests {

	@Test
	void emptyColumnStartsAtTheGap() {
		assertThat(Positions.between(null, null)).isEqualTo(1024);
	}

	@Test
	void topAndBottomOfAColumnAreOneGapAway() {
		assertThat(Positions.between(null, 1024.0)).isEqualTo(0);
		assertThat(Positions.between(2048.0, null)).isEqualTo(3072);
	}

	@Test
	void betweenTwoCardsIsTheMidpoint() {
		assertThat(Positions.between(1024.0, 2048.0)).isEqualTo(1536);
	}

	@Test
	void repeatedInsertionsEventuallyRequireRenumbering() {
		double above = 1024;
		double below = 2048;
		int insertions = 0;
		while (!Positions.needsRenumbering(above, below)) {
			below = Positions.between(above, below);
			insertions++;
		}
		// About 30 drops into the same slot before the column has to be renumbered.
		assertThat(insertions).isEqualTo(30);
		assertThat(Positions.needsRenumbering(2048.0, 1024.0)).isTrue();
		assertThat(Positions.needsRenumbering(null, 1024.0)).isFalse();
	}

}
