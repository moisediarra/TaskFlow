package com.xdsdata.taskflow.management.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityAction;
import com.xdsdata.taskflow.activity.ActivityService;
import com.xdsdata.taskflow.auth.AccountSecurity;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.common.error.ConflictException;
import com.xdsdata.taskflow.management.internal.ManagementDtos.UserRowDto;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import com.xdsdata.taskflow.users.UserStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role changes and account deactivation (claude.md §31). Both need explicit authorization: the IT Manager
 * re-enters their password, cannot target themselves, and the last active IT Manager can never be removed.
 * Deactivation is the "deletion": the account can no longer sign in, but its history is kept.
 */
@Service
class UserAdminService {

	private final UserService users;

	private final AccountSecurity accountSecurity;

	private final ActivityService activity;

	private final ManagementService management;

	UserAdminService(UserService users, AccountSecurity accountSecurity, ActivityService activity,
			ManagementService management) {
		this.users = users;
		this.accountSecurity = accountSecurity;
		this.activity = activity;
		this.management = management;
	}

	@Transactional
	UserRowDto changeRole(AuthUser actor, UUID userId, Role role, String currentPassword) {
		User target = authorize(actor, userId, currentPassword);
		Role previous = target.getRole();
		if (previous != role) {
			if (previous == Role.IT_MANAGER && target.isActive()) {
				requireAnotherItManager();
			}
			users.changeRole(userId, role);
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("targetUserId", userId.toString());
			metadata.put("targetUserName", target.getName());
			metadata.put("oldRole", previous.name());
			metadata.put("newRole", role.name());
			activity.record(actor, ActivityAction.USER_ROLE_CHANGED, null, null,
					"%s changed %s's role from %s to %s.".formatted(actor.name(), target.getName(), previous.label(),
							role.label()),
					metadata);
		}
		return management.row(users.getById(userId));
	}

	@Transactional
	UserRowDto changeStatus(AuthUser actor, UUID userId, UserStatus status, String currentPassword) {
		User target = authorize(actor, userId, currentPassword);
		UserStatus previous = target.getStatus();
		if (previous != status) {
			if (status == UserStatus.DEACTIVATED && target.getRole() == Role.IT_MANAGER) {
				requireAnotherItManager();
			}
			users.changeStatus(userId, status);
			if (status == UserStatus.DEACTIVATED) {
				accountSecurity.revokeAllSessions(userId);
			}
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("targetUserId", userId.toString());
			metadata.put("targetUserName", target.getName());
			metadata.put("oldAccountStatus", previous.name());
			metadata.put("newAccountStatus", status.name());
			String verb = status == UserStatus.DEACTIVATED ? "deactivated" : "reactivated";
			activity.record(actor, ActivityAction.USER_STATUS_CHANGED, null, null,
					"%s %s %s's account.".formatted(actor.name(), verb, target.getName()), metadata);
		}
		return management.row(users.getById(userId));
	}

	private User authorize(AuthUser actor, UUID userId, String currentPassword) {
		if (actor.id().equals(userId)) {
			throw new BadRequestException("CANNOT_CHANGE_SELF", "You can't change your own role or account status.");
		}
		User target = users.getById(userId);
		accountSecurity.verifyPassword(actor.id(), currentPassword);
		return target;
	}

	private void requireAnotherItManager() {
		if (users.lockActiveItManagers().size() <= 1) {
			throw new ConflictException("LAST_IT_MANAGER", "TaskFlow needs at least one active IT Manager.");
		}
	}

}
