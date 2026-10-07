package com.xdsdata.taskflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Guards the modular-monolith structure: no cycles between modules and no access to another module's internals.
 */
class ModularityTests {

	@Test
	void modulesRespectTheirBoundaries() {
		ApplicationModules.of(TaskFlowApplication.class).verify();
	}

}
