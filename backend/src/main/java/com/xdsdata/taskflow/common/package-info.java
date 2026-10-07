/**
 * Shared kernel used by every module: configuration, the authenticated principal, error handling and web helpers.
 * Declared open so its sub-packages are visible to all modules.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.xdsdata.taskflow.common;

import org.springframework.modulith.ApplicationModule;
