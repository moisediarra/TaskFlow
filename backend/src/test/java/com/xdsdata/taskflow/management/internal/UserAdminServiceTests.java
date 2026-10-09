package com.xdsdata.taskflow.management.internal;

import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityService;
import com.xdsdata.taskflow.auth.AccountSecurity;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.common.error.ConflictException;
import com.xdsdata.taskflow.projects.ProjectService;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import com.xdsdata.taskflow.users.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** TaskFlow always keeps at least one active IT Manager, and deleting an account never orphans projects. */
@ExtendWith(MockitoExtension.class)
class UserAdminServiceTests {

	@Mock
	private UserService users;

	@Mock
	private AccountSecurity accountSecurity;

	@Mock
	private ActivityService activity;

	@Mock
	private ManagementService management;

	@Mock
	private ProjectService projects;

	@InjectMocks
	private UserAdminService userAdmin;

	private final AuthUser actor = new AuthUser(UUID.randomUUID(), "Admin", "admin@test.local", Role.IT_MANAGER);

	@Test
	void theLastActiveItManagerCannotBeDemoted() {
		User last = itManager();
		when(users.getById(last.getId())).thenReturn(last);
		when(users.lockActiveItManagers()).thenReturn(List.of(last));
		assertThatThrownBy(() -> userAdmin.changeRole(actor, last.getId(), Role.MEMBER, "password"))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("at least one active IT Manager");
		verify(users, never()).changeRole(any(), any());
	}

	@Test
	void theLastActiveItManagerCannotBeDeactivated() {
		User last = itManager();
		when(users.getById(last.getId())).thenReturn(last);
		when(users.lockActiveItManagers()).thenReturn(List.of(last));
		assertThatThrownBy(() -> userAdmin.changeStatus(actor, last.getId(), UserStatus.DEACTIVATED, "password"))
			.isInstanceOf(ConflictException.class);
		verify(accountSecurity, never()).revokeAllSessions(any());
	}

	@Test
	void theLastActiveItManagerCannotBeDeleted() {
		User last = itManager();
		when(users.getById(last.getId())).thenReturn(last);
		when(users.lockActiveItManagers()).thenReturn(List.of(last));
		assertThatThrownBy(() -> userAdmin.delete(actor, last.getId(), "password"))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("at least one active IT Manager");
		verify(projects, never()).removeFromAllProjects(any(), any());
		verify(users, never()).delete(any());
	}

	@Test
	void projectOwnersAreNotDeletedSoTheirProjectsKeepAnOwner() {
		User owner = mock(User.class);
		UUID ownerId = UUID.randomUUID();
		when(owner.getName()).thenReturn("Olivia");
		when(users.getById(ownerId)).thenReturn(owner);
		when(management.ownedProjectCount(ownerId)).thenReturn(2L);
		assertThatThrownBy(() -> userAdmin.delete(actor, ownerId, "password"))
			.isInstanceOf(ConflictException.class)
			.hasMessage("Olivia owns 2 projects. Delete them first, or deactivate the account instead.");
		verify(users, never()).delete(any());
	}

	private static User itManager() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(UUID.randomUUID());
		when(user.getRole()).thenReturn(Role.IT_MANAGER);
		org.mockito.Mockito.lenient().when(user.isActive()).thenReturn(true);
		org.mockito.Mockito.lenient().when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
		return user;
	}

}
