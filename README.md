# TaskFlow
 # TaskFlow — Complete Product & Technical Requirements

 ## 1\. Project Overview

 Build a modern, simple, and professional **Task Management and Project Collaboration application** called **TaskFlow**.

 TaskFlow is inspired by tools such as Trello, but it must remain significantly simpler and more focused.

 The application is designed to help teams:

 - Organize projects.
- Manage tasks.
- Assign work to team members.
- Track task progress.
- Monitor deadlines.
- Communicate important task updates through notifications.
- Give IT Managers a global overview of team activity and workload.

 The application must NOT attempt to reproduce all Trello features.

 The core concept is:

 > **Simple Project Management + Kanban + Task Tracking + Team Visibility**

---

 # 2\. Product Goals

 The main goals are:

 1. Make task management simple.
2. Provide a clear Kanban workflow.
3. Make it easy to know who is responsible for each task.
4. Notify users when an action concerns them.
5. Give IT Managers a global view of projects and team activity.
6. Help managers identify workload problems and overdue tasks.
7. Keep the interface clean, modern, and easy to understand.

---

 # 3\. User Roles

 The application must support three roles:

```
IT_MANAGER
PROJECT_OWNER
MEMBER
```

 ## 3.1 IT Manager

 The IT Manager has global visibility across the application.

 The IT Manager can:

 - View all users.
- View all projects.
- View all tasks.
- View all project members.
- View who is assigned to each task.
- View task status.
- View task priorities.
- View deadlines.
- View overdue tasks.
- View team workload.
- View project activity.
- View global activity logs.
- Search users, projects, tasks, and activities.
- Access the IT Management Dashboard.

 The IT Manager should have global visibility but should not automatically modify or delete users' work unless explicitly authorized.

---

 ## 3.2 Project Owner

 A Project Owner manages projects they own.

 They can:

 - Create projects.
- Edit projects.
- Delete their projects.
- Add members to their projects.
- Remove members from their projects.
- Create tasks.
- Assign tasks.
- Edit tasks.
- Delete tasks.
- Move tasks between columns.
- View project activity.

---

 ## 3.3 Member / Intervenant

 A Member is a participant in one or more projects.

 They can:

 - View projects they belong to.
- View tasks in their projects.
- View tasks assigned to them.
- Update tasks according to their permissions.
- Change the status of tasks they are responsible for.
- Receive notifications.
- Update task information when permitted.

 Members must not have global access to the organization.

---

 # 4\. Authentication

 The application must provide:

 - Registration.
- Login.
- Logout.
- Password reset.
- User profile.

 ## Registration

 Required fields:

 - Full name.
- Email.
- Password.
- Confirm password.

 ## Login

 Required fields:

 - Email.
- Password.

 After successful authentication, the user is redirected to the appropriate dashboard.

---

 # 5\. Application Navigation

 The main navigation should contain:

```
Dashboard
Projects
Notifications
Profile
Logout
```

 For IT Managers, add:

```
Management
```

 The Management section should contain:

```
Overview
Team Activity
Workload
Activity Logs
Users
Projects
```

---

 # 6\. Dashboard

 The standard user dashboard should provide a simple overview of their work.

 Example:

```
Good morning, John 👋

My Tasks

┌──────────────┐ ┌──────────────┐
│ Total Tasks  │ │ In Progress  │
│     24       │ │      7       │
└──────────────┘ └──────────────┘

┌──────────────┐ ┌──────────────┐
│ To Do        │ │ Completed    │
│     12       │ │      5       │
└──────────────┘ └──────────────┘

High Priority
• Fix payment API
• Complete dashboard

Due Today
• Update documentation
• Review authentication

Overdue
• Database migration
```

 The dashboard should display:

 - Total assigned tasks.
- To Do tasks.
- In Progress tasks.
- Completed tasks.
- High-priority tasks.
- Tasks due today.
- Overdue tasks.
- Recent projects.

---

 # 7\. Projects

 Users with the appropriate permissions can create projects.

 Examples:

 - Mobile Banking App.
- Customer Portal.
- Internal Tools.
- Website.
- CRM Project.

 ## Project fields

```
Project
- id
- name
- description
- ownerId
- createdAt
- updatedAt
```

 ## Project actions

 Authorized users can:

 - Create.
- Read.
- Update.
- Delete.

 Example project card:

```
Mobile Banking App

12 active tasks
6 members

Updated 2 hours ago
```

---

 # 8\. Project Members / Intervenants

 A project can contain multiple members.

 Example:

```
Mobile Banking App

Members

John Doe
Developer

Sarah Smith
UI/UX Designer

Mohammed Ali
Backend Developer
```

 The Project Owner can add or remove members.

 ## ProjectMember

```
ProjectMember
- id
- projectId
- userId
- role
- joinedAt
```

 Roles:

```
OWNER
MEMBER
```

 The IT Manager can view all project members.

---

 # 9\. Kanban Board

 Every project has a Kanban board.

 The board must contain exactly four default columns in the MVP:

```
BACKLOG
TODO
IN_PROGRESS
DONE
```

 Example:

```
┌────────────┐ ┌────────────┐ ┌──────────────┐ ┌────────────┐
│ Backlog    │ │ To Do      │ │ In Progress  │ │ Done       │
├────────────┤ ├────────────┤ ├──────────────┤ ├────────────┤
│ Task 1     │ │ Task 3     │ │ Task 5       │ │ Task 7     │
│ Task 2     │ │ Task 4     │ │ Task 6       │ │ Task 8     │
│            │ │            │ │              │ │            │
│ + Add Task │ │ + Add Task │ │ + Add Task   │ │            │
└────────────┘ └────────────┘ └──────────────┘ └────────────┘
```

---

 # 10\. Backlog

 The Backlog contains tasks that have been identified but are not yet ready to be worked on.

 Examples:

```
Backlog

• Improve notification system
• Add dark mode
• Optimize database queries
• Create mobile application
```

 The normal workflow is:

```
Backlog
   ↓
To Do
   ↓
In Progress
   ↓
Done
```

 However, users should be able to move tasks between any columns using drag and drop.

---

 # 11\. Drag and Drop

 Tasks must support drag and drop.

 When a task is moved:

 1. Update its status.
2. Update its position.
3. Persist the change to the database.
4. Update the UI immediately.
5. Create an activity log.
6. Notify the assigned user if relevant.

 Example:

```
To Do → In Progress
```

 The task status must be updated in the backend.

---

 # 12\. Tasks

 Each task must contain:

```
Task
- id
- title
- description
- status
- priority
- dueDate
- projectId
- assigneeId
- position
- createdAt
- updatedAt
```

 ## Task statuses

```
BACKLOG
TODO
IN_PROGRESS
DONE
```

 ## Task priorities

```
HIGH
MEDIUM
LOW
```

---

 # 13\. Task Creation

 Users with permission can create a task from a Kanban column.

 The creation form should contain:

 - Title.
- Description.
- Priority.
- Due date.
- Tags.
- Assignee.

 The status is automatically determined by the column from which the task was created.

 For example:

```
Add Task
```

 inside the In Progress column creates:

```
status = IN_PROGRESS
```

---

 # 14\. Task Details

 Clicking a task opens a modal or side panel.

 Example:

```
Fix Authentication API

Description:
Implement login and token refresh functionality.

Status:
In Progress

Priority:
High

Due Date:
October 15

Tags:
Backend
API
Authentication

Assignee:
John Doe

Created:
October 7

Last Updated:
October 7
```

 Available actions depend on the user's role and permissions.

---

 # 15\. Task Assignment

 A task can be assigned to one intervenant.

 Example:

```
Assignee

[ Sarah Smith ▼ ]
```

 When a task is assigned:

 1. Save the assignment.
2. Create an activity log.
3. Send a notification to the assigned user.

 Example:

```
Sarah Smith has been assigned to:
"Implement Login UI"
```

---

 # 16\. Priorities

 There are three priorities:

```
🔴 HIGH
🟡 MEDIUM
🟢 LOW
```

 The priority should be clearly visible on task cards.

 Example:

```
🔴 Fix payment API
🟡 Update documentation
🟢 Refactor button component
```

---

 # 17\. Tags

 Tasks can have multiple tags.

 Examples:

```
Frontend
Backend
Bug
Design
API
Documentation
Urgent
```

 Tags should appear as colored badges.

 Example:

```
Fix Authentication API

[Backend] [API] [Urgent]
```

---

 # 18\. Deadlines

 Tasks can have an optional due date.

 The interface must clearly indicate:

 - Upcoming.
- Due today.
- Overdue.
- Completed.

 Examples:

```
📅 Due today
📅 Due October 15
⚠️ Overdue
```

 Completed tasks must not be displayed as overdue.

---

 # 19\. Search

 The application must provide task search.

 Search should work across:

 - Task title.
- Task description.
- Tags.

 Example:

```
Search: API
```

 Results:

```
Fix Authentication API
Payment API Integration
Update API Documentation
```

 Search should be debounced and optimized.

 IT Managers should have a global search that also includes:

 - Users.
- Projects.
- Tasks.
- Activity logs.

---

 # 20\. Notifications

 The application must include an in-app notification system.

 Notifications are sent to users when an important action concerns them.

 Users should be notified when:

 - They are assigned to a task.
- They are removed from a task.
- Their assigned task is updated.
- Their assigned task changes status.
- Their task deadline is approaching.
- Their task becomes overdue.

 Notifications must be targeted only to relevant users.

---

 # 21\. Notification Center

 The main navigation should contain a notification icon.

 Example:

```
Dashboard    Projects       🔔 3       John
```

 Clicking the icon opens a notification panel.

 Example:

```
Notifications

🔵 Sarah assigned you a task
5 minutes ago

🔵 Your task deadline is tomorrow
2 hours ago

✓ Your task was moved to Done
Yesterday

Mark all as read
```

 Users can:

 - View notifications.
- Mark a notification as read.
- Mark all notifications as read.
- Click a notification to open the related task.
- See the unread notification count.

---

 # 22\. Notification Types

 The system should support:

```
TASK_ASSIGNED
TASK_UNASSIGNED
TASK_UPDATED
TASK_STATUS_CHANGED
TASK_DEADLINE_APPROACHING
TASK_OVERDUE
```

---

 # 23\. Notification Data Model

```
Notification
- id
- userId
- taskId
- projectId
- type
- title
- message
- isRead
- createdAt
```

---

 # 24\. IT Management Dashboard

 The IT Manager must have access to a dedicated:

 **IT Management Dashboard**

 The purpose is to answer:

 > Who is doing what?\
>  What is currently in progress?\
>  What is overdue?\
>  Who is overloaded?\
>  What changed recently?

 Example:

```
IT Management Dashboard

┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Users        │ │ Projects     │ │ Active Tasks │
│     24       │ │      8       │ │      47      │
└──────────────┘ └──────────────┘ └──────────────┘

┌──────────────┐ ┌──────────────┐
│ In Progress  │ │ Overdue      │
│      18      │ │      6       │
└──────────────┘ └──────────────┘
```

 The dashboard should display:

 - Total users.
- Active users.
- Total projects.
- Active projects.
- Total tasks.
- Backlog tasks.
- To Do tasks.
- In Progress tasks.
- Completed tasks.
- Overdue tasks.
- High-priority tasks.

---

 # 25\. Team Activity — "Who Is Doing What?"

 The IT Manager must have a dedicated **Team Activity** page.

 This page provides a clear view of what each team member is currently working on.

 Example:

```
Team Activity

┌────────────┬──────────────────────┬──────────────┬─────────────┐
│ User       │ Current Task         │ Project      │ Status      │
├────────────┼──────────────────────┼──────────────┼─────────────┤
│ John       │ Authentication API   │ Banking App  │ In Progress │
│ Sarah      │ Login UI             │ Banking App  │ In Progress │
│ Mohammed   │ Database Migration   │ CRM          │ To Do       │
│ David      │ Payment Integration  │ Banking App  │ Done        │
└────────────┴──────────────────────┴──────────────┴─────────────┘
```

 The IT Manager must be able to filter by:

 - User.
- Project.
- Status.
- Priority.
- Due date.

---

 # 26\. Team Workload

 The IT Manager must be able to see how many active tasks are assigned to each person.

 Example:

```
Team Workload

John
████████████████░░░░ 8 active tasks

Sarah
██████████░░░░░░░░░░ 5 active tasks

Mohammed
██████████████░░░░░░ 7 active tasks

David
████░░░░░░░░░░░░░░░░ 2 active tasks
```

 The system should highlight potential workload issues.

 Example:

```
⚠️ John has 8 active tasks.
⚠️ Sarah has 3 overdue tasks.
```

 This feature is intended to help the IT Manager identify workload imbalances and bottlenecks.

 It should not automatically judge employee performance.

---

 # 27\. Activity Log

 The application must maintain an activity log for meaningful business actions.

 Example:

```
Activity

10:42 AM
John moved "Authentication API"
from To Do → In Progress.

10:35 AM
Sarah updated "Login UI".

10:21 AM
Mohammed was assigned
"Database Migration".

09:58 AM
David completed
"Payment Integration".
```

 The IT Manager can view the activity history.

---

 # 28\. Activity Types

 The system should record:

```
PROJECT_CREATED
PROJECT_UPDATED
PROJECT_MEMBER_ADDED
PROJECT_MEMBER_REMOVED

TASK_CREATED
TASK_UPDATED
TASK_DELETED
TASK_ASSIGNED
TASK_UNASSIGNED
TASK_STATUS_CHANGED
TASK_PRIORITY_CHANGED
TASK_DUE_DATE_CHANGED
TASK_COMPLETED
```

 Do not record every click or page visit.

 Only meaningful business actions should be logged.

---

 # 29\. Activity Log Data Model

```
ActivityLog
- id
- userId
- projectId
- taskId
- action
- description
- metadata
- createdAt
```

 Example:

```
{
  "userId": "123",
  "projectId": "456",
  "taskId": "789",
  "action": "TASK_STATUS_CHANGED",
  "description": "John moved Authentication API from To Do to In Progress",
  "metadata": {
    "oldStatus": "TODO",
    "newStatus": "IN_PROGRESS"
  },
  "createdAt": "2026-10-07T10:42:00Z"
}
```

---

 # 30\. Project Monitoring

 The IT Manager can open any project and see:

```
Mobile Banking App

Members: 6

Tasks
────────────────────────
Backlog       12
To Do          8
In Progress    5
Done          24

Overdue        2
High Priority  4
```

 The page should also display:

```
Current Team Activity

John       → Authentication API       In Progress
Sarah      → Login UI                 In Progress
Mohammed   → Database Migration       To Do
David      → Payment Integration      Done
```

---

 # 31\. User Management

 The IT Manager should have a Users page.

 Example:

```
Users

┌────────────┬──────────────────┬───────────────┬────────────┐
│ Name       │ Email            │ Role          │ Status     │
├────────────┼──────────────────┼───────────────┼────────────┤
│ John Doe   │ john@example.com │ Member        │ Active     │
│ Sarah      │ sarah@example.com│ Member        │ Active     │
│ Mohammed   │ mo@example.com   │ Project Owner │ Active     │
└────────────┴──────────────────┴───────────────┴────────────┘
```

 The IT Manager should be able to:

 - View users.
- Search users.
- Filter users by role.
- View user details.
- View the projects a user belongs to.
- View the tasks assigned to a user.

 User deletion or role modification should require explicit authorization and confirmation.

---

 # 32\. Security and Permissions

 Authorization must be enforced on the backend.

 Do NOT rely only on hiding UI elements.

 Every protected API endpoint must verify the authenticated user's role and permissions.

 Example:

```
IT_MANAGER
→ Global access

PROJECT_OWNER
→ Own projects and project members

MEMBER
→ Projects and tasks they are authorized to access
```

 Unauthorized access must return:

```
403 Forbidden
```

 Users must never be able to access another project's data by manually changing an ID in the URL or API request.

---

 # 33\. Privacy

 The monitoring system is designed for **work visibility and project management**, not invasive employee surveillance.

 The IT Manager may see:

 - Assigned tasks.
- Projects.
- Task status.
- Task progress.
- Deadlines.
- Overdue tasks.
- Important work-related actions.
- Team workload.

 The system must NOT track:

 - Keystrokes.
- Screen activity.
- Browser history.
- Personal files.
- Activity outside the application.
- Private conversations unrelated to project work.

---

 # 34\. Responsive Design

 The application must work on:

 - Desktop.
- Tablet.
- Mobile.

 ## Desktop

 Display all four Kanban columns horizontally.

 ## Tablet

 Columns can become narrower.

 ## Mobile

 The Kanban board should support horizontal scrolling.

 Task creation and editing must remain comfortable on mobile.

---

 # 35\. UI / UX

 The interface should be:

 - Modern.
- Minimal.
- Clean.
- Professional.
- Responsive.
- Easy to understand.

 Suggested visual style:

 - Light background.
- White cards.
- Rounded corners.
- Subtle shadows.
- Clear typography.
- Consistent spacing.
- Simple icons.
- Clear priority colors.

 The product should have its own identity and must not look like a direct Trello clone.

---

 # 36\. Empty States

 Every page must have useful empty states.

 Example:

```
No projects yet.

Create your first project and start organizing your tasks.

+ Create Project
```

 Empty Kanban column:

```
No tasks here.

+ Add Task
```

 Empty notifications:

```
You're all caught up!

No new notifications.
```

---

 # 37\. Error Handling

 Handle:

 - Invalid login.
- Invalid registration.
- Network errors.
- Failed task creation.
- Failed task update.
- Failed task deletion.
- Unauthorized access.
- Invalid form fields.
- Database errors.

 Use clear user-friendly messages.

 Never expose raw technical errors to users.

---

 # 38\. Performance

 The application should feel fast.

 Requirements:

 - Optimistic UI updates where appropriate.
- Efficient database queries.
- Avoid unnecessary API requests.
- Debounce search.
- Efficient drag-and-drop updates.
- Pagination for large activity logs.
- Pagination or lazy loading for large user/task lists where appropriate.

---

 # 39\. Suggested Database Model

 ## User

```
User
- id
- name
- email
- passwordHash
- role
- avatar
- createdAt
- updatedAt
```

 ## Project

```
Project
- id
- name
- description
- ownerId
- createdAt
- updatedAt
```

 ## ProjectMember

```
ProjectMember
- id
- projectId
- userId
- role
- joinedAt
```

 ## Task

```
Task
- id
- title
- description
- status
- priority
- dueDate
- projectId
- assigneeId
- position
- createdAt
- updatedAt
```

 ## Tag

```
Tag
- id
- name
- color
- projectId
```

 ## TaskTag

```
TaskTag
- taskId
- tagId
```

 ## Notification

```
Notification
- id
- userId
- taskId
- projectId
- type
- title
- message
- isRead
- createdAt
```

 ## ActivityLog

```
ActivityLog
- id
- userId
- projectId
- taskId
- action
- description
- metadata
- createdAt
```

---

 # 40\. MVP Scope

 The MVP MUST include:

 ### Authentication

 - Registration.
- Login.
- Logout.
- Password reset.
- Profile.

 ### Project Management

 - Create project.
- Edit project.
- Delete project.
- View project.
- Project members.

 ### Task Management

 - Create task.
- Edit task.
- Delete task.
- Assign task.
- Priority.
- Tags.
- Due date.

 ### Kanban

 - Backlog.
- To Do.
- In Progress.
- Done.
- Drag and drop.

 ### Search

 - Task search.
- Global search for IT Manager.

 ### Notifications

 - Task assignment notifications.
- Task update notifications.
- Status change notifications.
- Deadline notifications.
- Overdue notifications.
- Notification center.

 ### IT Management

 - IT Management Dashboard.
- Team Activity.
- Team Workload.
- Activity Logs.
- User Management.
- Project monitoring.

 ### General

 - Responsive design.
- Error handling.
- Security.
- Backend authorization.
- Persistent database.

---

 # 41\. Features NOT Required in MVP

 Do NOT implement:

 - Gantt charts.
- Full calendar system.
- Advanced automation.
- Plugins.
- Slack integration.
- Google Drive integration.
- Video calls.
- Billing/subscriptions.
- AI assistant.
- Custom workflows.
- Custom columns.
- Advanced comments.
- File storage.
- Time tracking.
- Enterprise SSO.
- Complex reporting.
- Employee screen monitoring.
- Email/SMS/push notifications.

 These can be considered for future versions.

---

 # 42\. Future Features

 Possible future versions may include:

 - Custom Kanban columns.
- Subtasks.
- Comments.
- File attachments.
- Calendar view.
- Recurring tasks.
- Dark mode.
- Email notifications.
- Push notifications.
- Real-time collaboration.
- Advanced reports.
- Task templates.
- Advanced filtering.
- Time tracking.
- AI-assisted task management.

 Do not implement these features in the MVP.

---

 # 43\. Development Approach

 Build the application incrementally.

 Recommended implementation order:

 1. Project setup and architecture.
2. Database schema.
3. Authentication.
4. Role-based authorization.
5. User management.
6. Project management.
7. Project members.
8. Task CRUD.
9. Kanban board.
10. Drag and drop.
11. Task assignment.
12. Tags and priorities.
13. Deadlines.
14. Notifications.
15. Activity logging.
16. Standard user dashboard.
17. IT Management Dashboard.
18. Team Activity.
19. Team Workload.
20. Search.
21. Responsive UI.
22. Error handling.
23. Security review.
24. Testing.
25. Final cleanup and documentation.

---

 # 44\. Important Development Rules

 The implementation must follow these principles:

 - Keep the architecture clean and maintainable.
- Avoid unnecessary complexity.
- Use reusable components.
- Keep business logic out of UI components when possible.
- Validate data on both frontend and backend.
- Enforce authorization on the backend.
- Use proper database relationships.
- Keep API responses consistent.
- Handle loading, success, empty, and error states.
- Do not implement features outside this specification without explicit approval.

 The application should be built as a **real production-quality MVP**, not merely as a visual prototype.

---

 # 45\. Definition of Done

 The MVP is complete when the following workflow works from end to end:

```
User registers
      ↓
User logs in
      ↓
User creates a project
      ↓
User adds project members
      ↓
User creates a task
      ↓
Task is placed in Backlog
      ↓
Task is assigned to an intervenant
      ↓
Intervenant receives a notification
      ↓
Task moves to To Do
      ↓
Task moves to In Progress
      ↓
IT Manager sees who is working on it
      ↓
IT Manager sees the activity in Team Activity
      ↓
Task is completed
      ↓
Task moves to Done
      ↓
Activity is recorded
      ↓
Relevant users receive notifications
```

 The IT Manager must be able to open the application and quickly answer:

 > **Who is working on what?**

 > **What tasks are currently in progress?**

 > **Which tasks are overdue?**

 > **Who has too many active tasks?**

 > **What important activities happened recently?**

 The final application should remain simple enough for everyday use while providing sufficient visibility for IT management.

# 46\ Architecture Backend
Je recommande une architecture : Modular Monolith.

Stack technique :

Backend
Java 21 LTS

Spring Boot 3

Spring Web / Spring MVC — REST API

Spring Data JPA

Hibernate — ORM

Spring Security — authentification et autorisation

JWT — authentication stateless

Bean Validation — validation des données

PostgreSQL — base de données

Maven — gestion des dépendances

JUnit 5 + Mockito — tests backend

Frontend
React

TypeScript

Vite

React Router — navigation

TanStack Query — communication avec l'API et gestion du server state

React Hook Form + Zod — formulaires et validation

Tailwind CSS — styling

shadcn/ui — composants UI

dnd-kit — drag & drop du Kanban

Recharts — graphiques du dashboard IT Manager

Axios — communication HTTP avec Spring Boot

Temps réel
Pour les notifications :

Spring WebSocket

STOMP

SockJS si nécessaire

Cela permettra par exemple à Sarah de recevoir immédiatement :

🔔 You have been assigned to "Implement Login UI"

sans devoir rafraîchir la page.
