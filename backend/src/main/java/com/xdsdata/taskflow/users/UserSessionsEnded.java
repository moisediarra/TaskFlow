package com.xdsdata.taskflow.users;

import java.util.UUID;

/**
 * Published when every session of a user ends (password changed or reset, account deactivated or deleted, a
 * stolen refresh token detected). After commit, open real-time connections of that user are closed.
 */
public record UserSessionsEnded(UUID userId) {
}
