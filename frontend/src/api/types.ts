// API types mirroring the backend DTOs (dates are ISO strings; due dates are YYYY-MM-DD).

export type Role = 'IT_MANAGER' | 'PROJECT_OWNER' | 'MEMBER'
export type UserStatus = 'ACTIVE' | 'DEACTIVATED'
export type ProjectRole = 'OWNER' | 'MEMBER'
export type TaskStatus = 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'HIGH' | 'MEDIUM' | 'LOW'
export type DueState = 'NONE' | 'UPCOMING' | 'DUE_TODAY' | 'OVERDUE' | 'COMPLETED'

export type NotificationType =
  | 'TASK_ASSIGNED'
  | 'TASK_UNASSIGNED'
  | 'TASK_UPDATED'
  | 'TASK_STATUS_CHANGED'
  | 'TASK_DEADLINE_APPROACHING'
  | 'TASK_OVERDUE'

export type ActivityAction =
  | 'PROJECT_CREATED'
  | 'PROJECT_UPDATED'
  | 'PROJECT_DELETED'
  | 'PROJECT_MEMBER_ADDED'
  | 'PROJECT_MEMBER_REMOVED'
  | 'TASK_CREATED'
  | 'TASK_UPDATED'
  | 'TASK_DELETED'
  | 'TASK_ASSIGNED'
  | 'TASK_UNASSIGNED'
  | 'TASK_STATUS_CHANGED'
  | 'TASK_PRIORITY_CHANGED'
  | 'TASK_DUE_DATE_CHANGED'
  | 'TASK_COMPLETED'
  | 'USER_ROLE_CHANGED'
  | 'USER_STATUS_CHANGED'

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface CursorPage<T> {
  items: T[]
  nextCursor: string | null
}

// ---- users & auth ----

export interface Profile {
  id: string
  name: string
  email: string
  role: Role
  status: UserStatus
  jobTitle: string | null
  avatar: string | null
  createdAt: string
}

export interface Session {
  accessToken: string
  expiresIn: number
  user: Profile
}

export interface UserSummary {
  id: string
  name: string
  email: string
  jobTitle: string | null
  active: boolean
}

// ---- projects ----

export interface ProjectPermissions {
  canManage: boolean
  canCreateTasks: boolean
}

export interface ProjectSummary {
  id: string
  name: string
  description: string | null
  owner: UserSummary
  memberCount: number
  activeTaskCount: number
  totalTaskCount: number
  updatedAt: string
  myRole: ProjectRole | null
  canManage: boolean
}

export interface ProjectDetail {
  id: string
  name: string
  description: string | null
  owner: UserSummary
  memberCount: number
  createdAt: string
  updatedAt: string
  myRole: ProjectRole | null
  permissions: ProjectPermissions
}

export interface ProjectMember {
  userId: string
  name: string
  email: string
  jobTitle: string | null
  active: boolean
  role: ProjectRole
  joinedAt: string
}

// ---- tasks & board ----

export interface Tag {
  id: string
  name: string
  color: string
}

export interface Assignee {
  id: string
  name: string
  jobTitle: string | null
  active: boolean
}

export interface TaskPermissions {
  canEdit: boolean
  canMove: boolean
  canDelete: boolean
  canAssign: boolean
}

export interface TaskCard {
  id: string
  title: string
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  dueState: DueState
  position: number
  assignee: Assignee | null
  tags: Tag[]
  permissions: TaskPermissions
  updatedAt: string
}

export interface TaskDetail {
  id: string
  projectId: string
  projectName: string
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  dueState: DueState
  assignee: Assignee | null
  tags: Tag[]
  permissions: TaskPermissions
  createdAt: string
  updatedAt: string
}

export interface BoardColumn {
  status: TaskStatus
  label: string
  tasks: TaskCard[]
  totalCount: number
}

export interface Board {
  project: { id: string; name: string; description: string | null; ownerId: string; ownerName: string }
  permissions: ProjectPermissions
  members: ProjectMember[]
  tags: Tag[]
  columns: BoardColumn[]
}

export interface TaskSlice {
  items: TaskCard[]
  hasMore: boolean
}

export interface TaskInput {
  title: string
  description: string | null
  priority: TaskPriority
  dueDate: string | null
  tagIds: string[]
  newTags: string[]
}

export interface NewTaskInput extends TaskInput {
  status: TaskStatus
  assigneeId: string | null
}

// ---- activity & notifications ----

export interface Activity {
  id: string
  action: ActivityAction
  description: string
  actorId: string | null
  actorName: string | null
  projectId: string | null
  projectName: string | null
  taskId: string | null
  taskTitle: string | null
  metadata: Record<string, unknown>
  createdAt: string
}

export interface AppNotification {
  id: string
  type: NotificationType
  title: string
  message: string
  taskId: string | null
  projectId: string | null
  read: boolean
  createdAt: string
}

export interface NotificationPush {
  notification: AppNotification
  unreadCount: number
}

// ---- dashboard & project monitoring ----

export interface StatusCounts {
  total: number
  backlog: number
  todo: number
  inProgress: number
  done: number
}

export interface TaskRef {
  id: string
  title: string
  projectId: string
  projectName: string
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  dueState: DueState
}

export interface TaskList {
  items: TaskRef[]
  total: number
}

export interface Dashboard {
  myTasks: StatusCounts
  highPriority: TaskList
  dueToday: TaskList
  overdue: TaskList
  recentProjects: ProjectSummary[]
}

export interface ProjectWorkItem {
  taskId: string
  title: string
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  dueState: DueState
  assigneeId: string
  assigneeName: string
  assigneeJobTitle: string | null
  updatedAt: string
}

export interface ProjectOverview {
  id: string
  name: string
  description: string | null
  owner: UserSummary
  memberCount: number
  tasks: StatusCounts
  overdue: number
  highPriority: number
  currentActivity: ProjectWorkItem[]
  members: ProjectMember[]
  recentActivity: Activity[]
  permissions: ProjectPermissions
}

// ---- IT management ----

export interface WorkItem {
  taskId: string
  title: string
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  dueState: DueState
  projectId: string
  projectName: string
  assigneeId: string | null
  assigneeName: string | null
  assigneeJobTitle: string | null
  updatedAt: string
}

export interface WorkloadRow {
  userId: string
  name: string
  jobTitle: string | null
  active: number
  inProgress: number
  todo: number
  overdue: number
  manyActive: boolean
  manyOverdue: boolean
}

export interface Workload {
  rows: WorkloadRow[]
  warnings: string[]
  activeTaskWarning: number
  overdueTaskWarning: number
  scaleMax: number
}

export interface ManagementOverview {
  users: { total: number; active: number }
  projects: { total: number; active: number }
  tasks: {
    total: number
    backlog: number
    todo: number
    inProgress: number
    done: number
    active: number
    overdue: number
    highPriority: number
  }
  openByPriority: { priority: TaskPriority; count: number }[]
  inProgressNow: WorkItem[]
  overdueTasks: WorkItem[]
  topWorkload: WorkloadRow[]
  workloadWarnings: string[]
  recentActivity: Activity[]
}

export interface UserRow {
  id: string
  name: string
  email: string
  role: Role
  status: UserStatus
  jobTitle: string | null
  createdAt: string
  projectCount: number
  activeTaskCount: number
}

export interface UserDetail {
  user: UserRow
  ownedProjectCount: number
  overdueTaskCount: number
  projects: { projectId: string; projectName: string; role: ProjectRole; joinedAt: string }[]
  tasks: WorkItem[]
}

// ---- search ----

export interface SearchResults {
  query: string
  tasks: {
    id: string
    title: string
    projectId: string
    projectName: string
    status: TaskStatus
    priority: TaskPriority
    tags: string[]
  }[]
  users: { id: string; name: string; email: string; role: Role; status: UserStatus; jobTitle: string | null }[] | null
  projects: { id: string; name: string; description: string | null }[] | null
  activities: Activity[] | null
}
