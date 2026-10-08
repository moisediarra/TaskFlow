import { format, formatDistanceToNowStrict, isThisYear, isYesterday, parseISO } from 'date-fns'
import type { DueState } from '@/api/types'

/** Calendar day from a YYYY-MM-DD string, at local midnight (never shifted by the time zone). */
export function parseDay(day: string): Date {
  return parseISO(day)
}

/** "Oct 15", or "Oct 15, 2027" outside the current year. */
export function formatDay(day: string): string {
  const date = parseDay(day)
  return format(date, isThisYear(date) ? 'MMM d' : 'MMM d, yyyy')
}

/** "October 15" — used in the task details panel (claude.md §14). */
export function formatLongDay(value: string): string {
  const date = value.length === 10 ? parseDay(value) : new Date(value)
  return format(date, isThisYear(date) ? 'MMMM d' : 'MMMM d, yyyy')
}

export function formatDateTime(iso: string): string {
  return format(new Date(iso), 'MMM d, yyyy, h:mm a')
}

export function formatTime(iso: string): string {
  return format(new Date(iso), 'h:mm a')
}

/** "5 minutes ago", "Yesterday", "3 days ago". */
export function timeAgo(iso: string): string {
  const date = new Date(iso)
  if (Date.now() - date.getTime() < 45_000) return 'Just now'
  if (isYesterday(date)) return 'Yesterday'
  return formatDistanceToNowStrict(date, { addSuffix: true })
}

/** Greeting for the dashboard, in the viewer's local time. */
export function greeting(now: Date = new Date()): string {
  const hour = now.getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
}

/** Deadline wording of claude.md §18. Completed tasks are never overdue. */
export function dueLabel(dueDate: string | null, dueState: DueState): string | null {
  switch (dueState) {
    case 'DUE_TODAY':
      return 'Due today'
    case 'OVERDUE':
      return dueDate ? `Overdue · ${formatDay(dueDate)}` : 'Overdue'
    case 'COMPLETED':
      return dueDate ? `Completed · was due ${formatDay(dueDate)}` : null
    case 'UPCOMING':
      return dueDate ? `Due ${formatDay(dueDate)}` : null
    default:
      return null
  }
}

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  const first = parts[0][0] ?? ''
  const last = parts.length > 1 ? (parts[parts.length - 1][0] ?? '') : ''
  return (first + last).toUpperCase()
}

export function pluralize(count: number, singular: string, plural = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : plural}`
}
