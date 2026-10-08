import type { ActivityLogFilters, TeamActivityFilters } from '@/api/endpoints'

/** TanStack Query cache keys, grouped so related data can be invalidated together. */
export const queryKeys = {
  dashboard: ['dashboard'] as const,
  projects: ['projects'] as const,
  project: (id: string) => ['projects', id] as const,
  board: (id: string) => ['projects', id, 'board'] as const,
  doneColumn: (id: string) => ['projects', id, 'done'] as const,
  overview: (id: string) => ['projects', id, 'overview'] as const,
  projectActivity: (id: string) => ['projects', id, 'activity'] as const,
  members: (id: string) => ['projects', id, 'members'] as const,
  task: (id: string) => ['tasks', id] as const,
  notifications: ['notifications'] as const,
  unreadCount: ['notifications', 'unread'] as const,
  search: (q: string) => ['search', q] as const,
  management: {
    all: ['management'] as const,
    overview: ['management', 'overview'] as const,
    teamActivity: (filters: TeamActivityFilters) => ['management', 'team-activity', filters] as const,
    workload: ['management', 'workload'] as const,
    activityLogs: (filters: ActivityLogFilters) => ['management', 'activity-logs', filters] as const,
    users: (filters: object) => ['management', 'users', filters] as const,
    user: (id: string) => ['management', 'users', id] as const,
    projects: (filters: object) => ['management', 'projects', filters] as const,
  },
}
