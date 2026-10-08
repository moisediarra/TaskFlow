import type { ActivityAction, Role, TaskPriority, TaskStatus } from '@/api/types'

export const STATUSES: TaskStatus[] = ['BACKLOG', 'TODO', 'IN_PROGRESS', 'DONE']

export const STATUS_LABEL: Record<TaskStatus, string> = {
  BACKLOG: 'Backlog',
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  DONE: 'Done',
}

/** Small colored dot per column; kept muted so priorities stay the loudest color on a card. */
export const STATUS_DOT: Record<TaskStatus, string> = {
  BACKLOG: 'bg-slate-400',
  TODO: 'bg-sky-500',
  IN_PROGRESS: 'bg-amber-500',
  DONE: 'bg-emerald-500',
}

export const STATUS_BADGE: Record<TaskStatus, string> = {
  BACKLOG: 'bg-slate-100 text-slate-700',
  TODO: 'bg-sky-50 text-sky-700',
  IN_PROGRESS: 'bg-amber-50 text-amber-800',
  DONE: 'bg-emerald-50 text-emerald-700',
}

export const PRIORITIES: TaskPriority[] = ['HIGH', 'MEDIUM', 'LOW']

export const PRIORITY_LABEL: Record<TaskPriority, string> = {
  HIGH: 'High',
  MEDIUM: 'Medium',
  LOW: 'Low',
}

/** 🔴 High · 🟡 Medium · 🟢 Low (claude.md §16). */
export const PRIORITY_EMOJI: Record<TaskPriority, string> = {
  HIGH: '🔴',
  MEDIUM: '🟡',
  LOW: '🟢',
}

export const PRIORITY_BADGE: Record<TaskPriority, string> = {
  HIGH: 'bg-red-50 text-red-700 ring-red-600/15',
  MEDIUM: 'bg-amber-50 text-amber-800 ring-amber-600/20',
  LOW: 'bg-emerald-50 text-emerald-700 ring-emerald-600/15',
}

export const ROLES: Role[] = ['IT_MANAGER', 'PROJECT_OWNER', 'MEMBER']

export const ROLE_LABEL: Record<Role, string> = {
  IT_MANAGER: 'IT Manager',
  PROJECT_OWNER: 'Project Owner',
  MEMBER: 'Member',
}

/** Tag palette keys chosen by the server, mapped to badge classes (full class names for Tailwind). */
export const TAG_COLORS: Record<string, string> = {
  teal: 'bg-teal-50 text-teal-700 ring-teal-600/20',
  sky: 'bg-sky-50 text-sky-700 ring-sky-600/20',
  indigo: 'bg-indigo-50 text-indigo-700 ring-indigo-600/20',
  violet: 'bg-violet-50 text-violet-700 ring-violet-600/20',
  fuchsia: 'bg-fuchsia-50 text-fuchsia-700 ring-fuchsia-600/20',
  rose: 'bg-rose-50 text-rose-700 ring-rose-600/20',
  orange: 'bg-orange-50 text-orange-700 ring-orange-600/20',
  amber: 'bg-amber-50 text-amber-800 ring-amber-600/20',
  lime: 'bg-lime-50 text-lime-800 ring-lime-600/20',
  emerald: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  slate: 'bg-slate-100 text-slate-700 ring-slate-600/20',
}

export const ACTIVITY_LABEL: Record<ActivityAction, string> = {
  PROJECT_CREATED: 'Project created',
  PROJECT_UPDATED: 'Project updated',
  PROJECT_DELETED: 'Project deleted',
  PROJECT_MEMBER_ADDED: 'Member added',
  PROJECT_MEMBER_REMOVED: 'Member removed',
  TASK_CREATED: 'Task created',
  TASK_UPDATED: 'Task updated',
  TASK_DELETED: 'Task deleted',
  TASK_ASSIGNED: 'Task assigned',
  TASK_UNASSIGNED: 'Task unassigned',
  TASK_STATUS_CHANGED: 'Status changed',
  TASK_PRIORITY_CHANGED: 'Priority changed',
  TASK_DUE_DATE_CHANGED: 'Due date changed',
  TASK_COMPLETED: 'Task completed',
  USER_ROLE_CHANGED: 'Role changed',
  USER_STATUS_CHANGED: 'Account status changed',
}

export const ACTIVITY_ACTIONS = Object.keys(ACTIVITY_LABEL) as ActivityAction[]
