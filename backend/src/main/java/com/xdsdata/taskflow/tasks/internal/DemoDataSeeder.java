package com.xdsdata.taskflow.tasks.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.projects.ProjectService;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.tasks.internal.TaskRequests.CreateTaskRequest;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Optional demo data (APP_SEED_DEMO=true) based on the examples in claude.md: a few people, three projects and
 * tasks spread over the board, some due today and some overdue. Everything goes through the regular services,
 * so activity history and notifications are produced as in real use. Runs only on an empty database.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
class DemoDataSeeder implements ApplicationRunner {

	static final String DOMAIN = "@taskflow.local";

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	private final UserService users;

	private final ProjectService projects;

	private final TaskService tasks;

	private final PasswordEncoder passwordEncoder;

	private final TransactionTemplate transaction;

	private final AppProperties properties;

	private final Clock clock;

	DemoDataSeeder(UserService users, ProjectService projects, TaskService tasks, PasswordEncoder passwordEncoder,
			TransactionTemplate transaction, AppProperties properties, Clock clock) {
		this.users = users;
		this.projects = projects;
		this.tasks = tasks;
		this.passwordEncoder = passwordEncoder;
		this.transaction = transaction;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public void run(ApplicationArguments args) {
		String password = properties.seed().demoPassword();
		if (password == null || password.length() < 8) {
			log.warn("APP_SEED_DEMO is enabled but APP_SEED_DEMO_PASSWORD is missing or shorter than 8 characters; "
					+ "no demo data was created.");
			return;
		}
		if (projects.anyProjectExists() || users.findByEmail("john.doe" + DOMAIN).isPresent()) {
			return;
		}
		transaction.executeWithoutResult(status -> seed(passwordEncoder.encode(password)));
		log.info("Demo data created: users *{} share APP_SEED_DEMO_PASSWORD.", DOMAIN);
	}

	private void seed(String passwordHash) {
		LocalDate today = LocalDate.now(clock);
		AuthUser mohammed = person("Mohammed Ali", "mohammed.ali", Role.PROJECT_OWNER, "Backend Developer", passwordHash);
		AuthUser emma = person("Emma Wilson", "emma.wilson", Role.PROJECT_OWNER, "Product Manager", passwordHash);
		AuthUser john = person("John Doe", "john.doe", Role.MEMBER, "Developer", passwordHash);
		AuthUser sarah = person("Sarah Smith", "sarah.smith", Role.MEMBER, "UI/UX Designer", passwordHash);
		AuthUser david = person("David Martin", "david.martin", Role.MEMBER, "QA Engineer", passwordHash);

		UUID banking = projects.create(mohammed, "Mobile Banking App",
				"Customer-facing banking app: accounts, payments and secure sign-in.").id();
		for (AuthUser member : List.of(john, sarah, david)) {
			projects.addMember(mohammed, banking, member.email());
		}
		UUID authApi = task(mohammed, banking, "Authentication API", "Implement login and token refresh functionality.",
				TaskPriority.HIGH, today.plusDays(5), TaskStatus.TODO, john, "Backend", "API");
		tasks.move(john, authApi, TaskStatus.IN_PROGRESS, null, null);
		task(mohammed, banking, "Login UI", "Sign-in and password reset screens for the mobile app.", TaskPriority.HIGH,
				today.plusDays(1), TaskStatus.IN_PROGRESS, sarah, "Frontend", "Design");
		UUID payment = task(mohammed, banking, "Payment Integration", "Connect the card payment provider.",
				TaskPriority.MEDIUM, today.minusDays(2), TaskStatus.IN_PROGRESS, david, "Backend", "API");
		tasks.move(david, payment, TaskStatus.DONE, null, null);
		task(mohammed, banking, "Fix payment API", "Refunds fail when the amount has more than two decimals.",
				TaskPriority.HIGH, today, TaskStatus.TODO, john, "Bug", "API", "Urgent");
		task(mohammed, banking, "Update documentation", "Document the new authentication flow for the support team.",
				TaskPriority.MEDIUM, today, TaskStatus.TODO, sarah, "Documentation");
		task(mohammed, banking, "Review authentication", "Security review of the token refresh logic.",
				TaskPriority.MEDIUM, today, TaskStatus.TODO, david, "Backend");
		task(mohammed, banking, "Improve notification system", null, TaskPriority.LOW, null, TaskStatus.BACKLOG, null);
		task(mohammed, banking, "Add dark mode", null, TaskPriority.LOW, null, TaskStatus.BACKLOG, null, "Frontend",
				"Design");
		task(mohammed, banking, "Optimize database queries", null, TaskPriority.MEDIUM, null, TaskStatus.BACKLOG, null,
				"Backend");
		task(mohammed, banking, "Create mobile application", null, TaskPriority.LOW, null, TaskStatus.BACKLOG, null);

		UUID crm = projects.create(mohammed, "CRM Project", "Internal customer relationship management tool.").id();
		for (AuthUser member : List.of(john, david)) {
			projects.addMember(mohammed, crm, member.email());
		}
		task(mohammed, crm, "Database Migration", "Move customer records to the new schema.", TaskPriority.HIGH,
				today.minusDays(3), TaskStatus.TODO, mohammed, "Backend");
		task(mohammed, crm, "Customer import", "CSV import for existing customer lists.", TaskPriority.MEDIUM,
				today.plusDays(2), TaskStatus.IN_PROGRESS, david, "Backend");
		task(mohammed, crm, "Refactor button component", null, TaskPriority.LOW, null, TaskStatus.TODO, john,
				"Frontend");

		UUID portal = projects.create(emma, "Customer Portal", "Self-service portal for business customers.").id();
		for (AuthUser member : List.of(sarah, john)) {
			projects.addMember(emma, portal, member.email());
		}
		task(emma, portal, "Portal landing page", "Hero, pricing and contact sections.", TaskPriority.MEDIUM,
				today.minusDays(1), TaskStatus.IN_PROGRESS, sarah, "Frontend", "Design");
		task(emma, portal, "Complete dashboard", "Account overview with invoices and usage.", TaskPriority.HIGH,
				today.plusDays(7), TaskStatus.TODO, john, "Frontend");
		task(emma, portal, "Accessibility review", null, TaskPriority.MEDIUM, null, TaskStatus.BACKLOG, null);
	}

	private AuthUser person(String name, String localPart, Role role, String jobTitle, String passwordHash) {
		User user = users.create(name, localPart + DOMAIN, passwordHash, role, jobTitle);
		return user.toAuthUser();
	}

	private UUID task(AuthUser owner, UUID projectId, String title, String description, TaskPriority priority,
			LocalDate dueDate, TaskStatus status, AuthUser assignee, String... tagNames) {
		CreateTaskRequest request = new CreateTaskRequest(title, description, priority, dueDate, status,
				assignee == null ? null : assignee.id(), List.of(), List.of(tagNames));
		return tasks.create(owner, projectId, request).id();
	}

}
