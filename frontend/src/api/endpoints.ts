import { authHttp, http } from './client'
import type {
  Activity,
  ActivityAction,
  AppNotification,
  Board,
  CursorPage,
  Dashboard,
  ManagementOverview,
  NewTaskInput,
  PageResponse,
  Profile,
  ProjectDetail,
  ProjectMember,
  ProjectOverview,
  ProjectSummary,
  Role,
  SearchResults,
  Session,
  Tag,
  TaskCard,
  TaskDetail,
  TaskInput,
  TaskPriority,
  TaskSlice,
  TaskStatus,
  UserDetail,
  UserRow,
  UserStatus,
  WorkItem,
  Workload,
} from './types'

const data = <T>(promise: Promise<{ data: T }>) => promise.then((response) => response.data)

/** Drops empty values so they are not sent as query parameters. */
function params(values: Record<string, unknown>) {
  return Object.fromEntries(Object.entries(values).filter(([, value]) => value !== undefined && value !== null && value !== ''))
}

export const authApi = {
  register: (body: { name: string; email: string; password: string; confirmPassword: string }) =>
    data(authHttp.post<Profile>('/auth/register', body)),
  login: (body: { email: string; password: string }) => data(authHttp.post<Session>('/auth/login', body)),
  logout: () => authHttp.post('/auth/logout'),
  forgotPassword: (email: string) => data(authHttp.post<{ message: string }>('/auth/forgot-password', { email })),
  resetPassword: (body: { token: string; password: string; confirmPassword: string }) =>
    authHttp.post('/auth/reset-password', body),
  me: () => data(http.get<Profile>('/auth/me')),
  updateProfile: (body: { name: string; jobTitle: string | null }) => data(http.patch<Profile>('/users/me', body)),
  changePassword: (body: { currentPassword: string; newPassword: string; confirmPassword: string }) =>
    data(http.post<Session>('/users/me/password', body)),
}

export const projectsApi = {
  list: () => data(http.get<ProjectSummary[]>('/projects')),
  get: (id: string) => data(http.get<ProjectDetail>(`/projects/${id}`)),
  create: (body: { name: string; description: string | null }) => data(http.post<ProjectDetail>('/projects', body)),
  update: (id: string, body: { name: string; description: string | null }) =>
    data(http.put<ProjectDetail>(`/projects/${id}`, body)),
  remove: (id: string) => http.delete(`/projects/${id}`),
  members: (id: string) => data(http.get<ProjectMember[]>(`/projects/${id}/members`)),
  addMember: (id: string, email: string) => data(http.post<ProjectMember>(`/projects/${id}/members`, { email })),
  removeMember: (id: string, userId: string) => http.delete(`/projects/${id}/members/${userId}`),
  overview: (id: string) => data(http.get<ProjectOverview>(`/projects/${id}/overview`)),
  activity: (id: string, cursor?: string | null) =>
    data(http.get<CursorPage<Activity>>(`/projects/${id}/activity`, { params: params({ cursor, limit: 20 }) })),
  tags: (id: string) => data(http.get<Tag[]>(`/projects/${id}/tags`)),
}

export const tasksApi = {
  board: (projectId: string) => data(http.get<Board>(`/projects/${projectId}/board`)),
  column: (projectId: string, status: TaskStatus, page: number) =>
    data(http.get<TaskSlice>(`/projects/${projectId}/tasks`, { params: { status, page, size: 50 } })),
  get: (taskId: string) => data(http.get<TaskDetail>(`/tasks/${taskId}`)),
  create: (projectId: string, body: NewTaskInput) => data(http.post<TaskDetail>(`/projects/${projectId}/tasks`, body)),
  update: (taskId: string, body: TaskInput) => data(http.put<TaskDetail>(`/tasks/${taskId}`, body)),
  assign: (taskId: string, assigneeId: string | null) =>
    data(http.put<TaskDetail>(`/tasks/${taskId}/assignee`, { assigneeId })),
  move: (taskId: string, body: { status: TaskStatus; previousTaskId: string | null; nextTaskId: string | null }) =>
    data(http.patch<TaskCard>(`/tasks/${taskId}/move`, body)),
  remove: (taskId: string) => http.delete(`/tasks/${taskId}`),
}

export const dashboardApi = {
  get: () => data(http.get<Dashboard>('/dashboard')),
}

export const notificationsApi = {
  list: (cursor?: string | null) =>
    data(http.get<CursorPage<AppNotification>>('/notifications', { params: params({ cursor, limit: 20 }) })),
  unreadCount: () => data(http.get<{ count: number }>('/notifications/unread-count')),
  markRead: (id: string) => http.patch(`/notifications/${id}/read`),
  markAllRead: () => http.post('/notifications/read-all'),
}

export const searchApi = {
  search: (q: string) => data(http.get<SearchResults>('/search', { params: { q } })),
}

export type DueFilter = 'ANY' | 'OVERDUE' | 'TODAY' | 'THIS_WEEK' | 'NONE'

export interface TeamActivityFilters {
  userId?: string
  projectId?: string
  status?: TaskStatus | 'ALL'
  priority?: TaskPriority
  due?: DueFilter
  page?: number
}

export interface ActivityLogFilters {
  userId?: string
  projectId?: string
  action?: ActivityAction
  from?: string
  to?: string
  q?: string
}

const ALL_STATUSES: TaskStatus[] = ['BACKLOG', 'TODO', 'IN_PROGRESS', 'DONE']

export const managementApi = {
  overview: () => data(http.get<ManagementOverview>('/management/overview')),
  teamActivity: ({ status, ...filters }: TeamActivityFilters) =>
    data(
      http.get<PageResponse<WorkItem>>('/management/team-activity', {
        params: params({
          ...filters,
          status: status === 'ALL' ? ALL_STATUSES.join(',') : status,
          size: 20,
        }),
      }),
    ),
  workload: () => data(http.get<Workload>('/management/workload')),
  activityLogs: (filters: ActivityLogFilters, cursor?: string | null) =>
    data(http.get<CursorPage<Activity>>('/management/activity-logs', { params: params({ ...filters, cursor, limit: 30 }) })),
  users: (filters: { q?: string; role?: Role; status?: UserStatus; page?: number; size?: number }) =>
    data(http.get<PageResponse<UserRow>>('/management/users', { params: params({ size: 20, ...filters }) })),
  user: (id: string) => data(http.get<UserDetail>(`/management/users/${id}`)),
  changeRole: (id: string, role: Role, currentPassword: string) =>
    data(http.patch<UserRow>(`/management/users/${id}/role`, { role, currentPassword })),
  changeStatus: (id: string, status: UserStatus, currentPassword: string) =>
    data(http.patch<UserRow>(`/management/users/${id}/status`, { status, currentPassword })),
  projects: (filters: { q?: string; page?: number; size?: number }) =>
    data(http.get<PageResponse<ProjectSummary>>('/management/projects', { params: params({ size: 20, ...filters }) })),
}
