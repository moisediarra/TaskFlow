package com.xdsdata.taskflow.notifications.internal;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.notifications.NotificationType;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskAssigneeChanged;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskMoved;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskSnapshot;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskUpdated;
import com.xdsdata.taskflow.tasks.TaskField;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import com.xdsdata.taskflow.users.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Who gets notified about what (claude.md §20). */
@ExtendWith(MockitoExtension.class)
class NotificationRulesTests {

	private static final UUID OWNER = UUID.randomUUID();

	private static final UUID ASSIGNEE = UUID.randomUUID();

	private static final AuthUser OWNER_ACTOR = new AuthUser(OWNER, "Olivia", "olivia@test.local", Role.PROJECT_OWNER);

	private static final AuthUser ASSIGNEE_ACTOR = new AuthUser(ASSIGNEE, "Ian", "ian@test.local", Role.MEMBER);

	@Mock
	private NotificationService notifications;

	@Mock
	private DeadlineNotifier deadlines;

	@Mock
	private UserService users;

	private NotificationRules rules;

	@BeforeEach
	void setUp() {
		rules = new NotificationRules(notifications, deadlines, users);
		lenient().when(users.findById(any())).thenAnswer(invocation -> Optional.of(activeUser()));
	}

	@Test
	void assigneeHearsAboutMovesByTheOwnerButTheOwnerDoesNot() {
		rules.on(new TaskMoved(OWNER_ACTOR, task(TaskStatus.IN_PROGRESS), TaskStatus.TODO, TaskStatus.IN_PROGRESS));
		verify(notifications).notify(eq(ASSIGNEE), eq(NotificationType.TASK_STATUS_CHANGED),
				eq("Your task was moved to In Progress"), anyString(), any(), any());
		verify(notifications, never()).notify(eq(OWNER), any(), anyString(), anyString(), any(), any());
	}

	@Test
	void ownerHearsWhenTheAssigneeCompletesTheTask() {
		rules.on(new TaskMoved(ASSIGNEE_ACTOR, task(TaskStatus.DONE), TaskStatus.IN_PROGRESS, TaskStatus.DONE));
		verify(notifications).notify(eq(OWNER), eq(NotificationType.TASK_STATUS_CHANGED), eq("Ian completed a task"),
				anyString(), any(), any());
		verify(notifications, never()).notify(eq(ASSIGNEE), any(), anyString(), anyString(), any(), any());
	}

	@Test
	void reassigningNotifiesBothThePreviousAndTheNewAssignee() {
		UUID newcomer = UUID.randomUUID();
		rules.on(new TaskAssigneeChanged(OWNER_ACTOR, task(TaskStatus.TODO), ASSIGNEE, "Ian", newcomer, "Nora", false));
		verify(notifications).notify(eq(ASSIGNEE), eq(NotificationType.TASK_UNASSIGNED), anyString(), anyString(), any(),
				any());
		verify(notifications).notify(eq(newcomer), eq(NotificationType.TASK_ASSIGNED), eq("Olivia assigned you a task"),
				anyString(), any(), any());
	}

	@Test
	void editsByTheAssigneeThemselvesNotifyNobody() {
		TaskSnapshot task = task(TaskStatus.TODO);
		rules.on(new TaskUpdated(ASSIGNEE_ACTOR, task, task, Set.of(TaskField.DESCRIPTION)));
		verifyNoInteractions(notifications);
	}

	@Test
	void deactivatedPeopleAreNeverNotified() {
		User deactivated = mock(User.class);
		when(deactivated.isActive()).thenReturn(false);
		when(users.findById(ASSIGNEE)).thenReturn(Optional.of(deactivated));
		TaskSnapshot task = task(TaskStatus.TODO);
		rules.on(new TaskUpdated(OWNER_ACTOR, task, task, Set.of(TaskField.PRIORITY)));
		verifyNoInteractions(notifications);
	}

	private static TaskSnapshot task(TaskStatus status) {
		return new TaskSnapshot(UUID.randomUUID(), "Implement Login UI", UUID.randomUUID(), "Banking App", OWNER, status,
				TaskPriority.HIGH, null, ASSIGNEE, "Ian", List.of());
	}

	private static User activeUser() {
		User user = mock(User.class);
		lenient().when(user.isActive()).thenReturn(true);
		lenient().when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
		return user;
	}

}
