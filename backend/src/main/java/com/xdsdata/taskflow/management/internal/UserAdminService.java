package com.xdsdata.taskflow.management.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityAction;
import com.xdsdata.taskflow.activity.ActivityService;
import com.xdsdata.taskflow.auth.AccountSecurity;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.common.error.ConflictException;
import com.xdsdata.taskflow.management.internal.ManagementDtos.UserRowDto;
import com.xdsdata.taskflow.management.internal.UserAdminRequests.CreateUserRequest;
import com.xdsdata.taskflow.management.internal.UserAdminRequests.UpdateUserRequest;
import com.xdsdata.taskflow.projects.ProjectService;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import com.xdsdata.taskflow.users.UserStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The IT Manager's account administration (claude.md §31): create accounts, correct their details, reset
 * passwords, change roles, deactivate, reactivate and delete. Every change needs explicit authorization (the
 * IT Manager re-enters their password) and is recorded in the activity log; passwords never are.
 * <ul>
 * <li>IT Managers can edit their own details here, but not their own role, status or password, and they
 * can't delete themselves.</li>
 * <li>TaskFlow always keeps at least one active IT Manager.</li>
 * <li>Deleting removes the account but keeps the person's work: their tasks stay (unassigned) and the
 * activity history keeps their name. People who own projects can't be deleted; deactivate them instead.</li>
 * </ul>
 */
@Service
class UserAdminService {

	private final UserService users;

	private final AccountSecurity accountSecurity;

	private final ActivityService activity;

	private final ManagementService management;

	private final ProjectService projects;

	UserAdminService(UserService users, AccountSecurity accountSecurity, ActivityService activity,
			ManagementService management, ProjectService projects) {
		this.users = users;
		this.accountSecurity = accountSecurity;
		this.activity = activity;
		this.management = management;
		this.projects = projects;
	}

	@Transactional
	UserRowDto create(AuthUser actor, CreateUserRequest request) {
		accountSecurity.verifyPassword(actor.id(), request.currentPassword());
		User user = accountSecurity.createAccount(request.name(), request.email(), request.password(),
				request.confirmPassword(), request.role(), request.jobTitle());
		Map<String, Object> metadata = about(user);
		metadata.put("role", user.getRole().name());
		activity.record(actor, ActivityAction.USER_CREATED, null, null,
				"%s created an account for %s (%s).".formatted(actor.name(), user.getName(), user.getRole().label()),
				metadata);
		return management.row(user);
	}

	@Transactional
	UserRowDto updateDetails(AuthUser actor, UUID userId, UpdateUserRequest request) {
		User target = users.getById(userId);
		accountSecurity.verifyPassword(actor.id(), request.currentPassword());
		String previousName = target.getName();
		String previousEmail = target.getEmail();
		String previousJobTitle = target.getJobTitle();
		User updated = users.updateDetails(userId, request.name(), request.email(), request.jobTitle());

		List<String> changed = new ArrayList<>();
		Map<String, Object> metadata = about(updated);
		if (!previousName.equals(updated.getName())) {
			changed.add("name");
			metadata.put("previousName", previousName);
		}
		if (!previousEmail.equals(updated.getEmail())) {
			changed.add("email");
			metadata.put("previousEmail", previousEmail);
			metadata.put("newEmail", updated.getEmail());
		}
		if (!Objects.equals(previousJobTitle, updated.getJobTitle())) {
			changed.add("job title");
		}
		if (!changed.isEmpty()) {
			metadata.put("changedFields", changed);
			activity.record(actor, ActivityAction.USER_UPDATED, null, null,
					"%s updated %s's account (%s).".formatted(actor.name(), previousName, String.join(", ", changed)),
					metadata);
		}
		return management.row(updated);
	}

	@Transactional
	UserRowDto changeRole(AuthUser actor, UUID userId, Role role, String currentPassword) {
		User target = authorize(actor, userId, currentPassword, "You can't change your own role.");
		Role previous = target.getRole();
		if (previous != role) {
			if (previous == Role.IT_MANAGER && target.isActive()) {
				requireAnotherItManager();
			}
			users.changeRole(userId, role);
			Map<String, Object> metadata = about(target);
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
		User target = authorize(actor, userId, currentPassword, "You can't change your own account status.");
		UserStatus previous = target.getStatus();
		if (previous != status) {
			if (status == UserStatus.DEACTIVATED && target.getRole() == Role.IT_MANAGER) {
				requireAnotherItManager();
			}
			users.changeStatus(userId, status);
			if (status == UserStatus.DEACTIVATED) {
				accountSecurity.revokeAllSessions(userId);
			}
			Map<String, Object> metadata = about(target);
			metadata.put("oldAccountStatus", previous.name());
			metadata.put("newAccountStatus", status.name());
			String verb = status == UserStatus.DEACTIVATED ? "deactivated" : "reactivated";
			activity.record(actor, ActivityAction.USER_STATUS_CHANGED, null, null,
					"%s %s %s's account.".formatted(actor.name(), verb, target.getName()), metadata);
		}
		return management.row(users.getById(userId));
	}

	/** Sets a new password the IT Manager shares with the person; their sessions end immediately. */
	@Transactional
	void resetPassword(AuthUser actor, UUID userId, String newPassword, String confirmation, String currentPassword) {
		User target = authorize(actor, userId, currentPassword, "To change your own password, use your profile.");
		accountSecurity.setPassword(userId, newPassword, confirmation);
		activity.record(actor, ActivityAction.USER_PASSWORD_RESET, null, null,
				"%s reset %s's password.".formatted(actor.name(), target.getName()), about(target));
	}

	@Transactional
	void delete(AuthUser actor, UUID userId, String currentPassword) {
		User target = authorize(actor, userId, currentPassword, "You can't delete your own account.");
		long owned = management.ownedProjectCount(userId);
		if (owned > 0) {
			throw new ConflictException("OWNS_PROJECTS",
					"%s owns %d %s. Delete %s first, or deactivate the account instead.".formatted(target.getName(), owned,
							owned == 1 ? "project" : "projects", owned == 1 ? "it" : "them"));
		}
		if (target.getRole() == Role.IT_MANAGER && target.isActive()) {
			requireAnotherItManager();
		}
		Map<String, Object> metadata = about(target);
		metadata.put("email", target.getEmail());
		metadata.put("role", target.getRole().name());
		String name = target.getName();
		projects.removeFromAllProjects(actor, userId);
		activity.record(actor, ActivityAction.USER_DELETED, null, null,
				"%s deleted %s's account.".formatted(actor.name(), name), metadata);
		users.delete(userId);
	}

	private User authorize(AuthUser actor, UUID userId, String currentPassword, String selfMessage) {
		if (actor.id().equals(userId)) {
			throw new BadRequestException("CANNOT_CHANGE_SELF", selfMessage);
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

	/** Who the change was about; the snapshot keeps the log readable after renames and deletions. */
	private static Map<String, Object> about(User user) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("targetUserId", user.getId().toString());
		metadata.put("targetUserName", user.getName());
		return metadata;
	}

}
