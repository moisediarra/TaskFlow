import { AlertTriangle, CalendarDays, CheckCircle2 } from 'lucide-react'
import type { DueState, Tag, TaskPriority, TaskStatus } from '@/api/types'
import { dueLabel } from '@/lib/format'
import { PRIORITY_BADGE, PRIORITY_EMOJI, PRIORITY_LABEL, STATUS_BADGE, STATUS_DOT, STATUS_LABEL, TAG_COLORS } from '@/lib/labels'
import { cn } from '@/lib/utils'

/** Compact priority marker for cards and lists (🔴 / 🟡 / 🟢). */
export function PriorityDot({ priority }: { priority: TaskPriority }) {
  return (
    <span role="img" aria-label={`${PRIORITY_LABEL[priority]} priority`} title={`${PRIORITY_LABEL[priority]} priority`} className="text-xs leading-none">
      {PRIORITY_EMOJI[priority]}
    </span>
  )
}

export function PriorityBadge({ priority, className }: { priority: TaskPriority; className?: string }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset',
        PRIORITY_BADGE[priority],
        className,
      )}
    >
      <span aria-hidden className="text-[10px] leading-none">
        {PRIORITY_EMOJI[priority]}
      </span>
      {PRIORITY_LABEL[priority]}
    </span>
  )
}

/** Deadline indicator (claude.md §18): upcoming, due today, overdue or completed. */
export function DueBadge({ dueDate, dueState, className }: { dueDate: string | null; dueState: DueState; className?: string }) {
  const label = dueLabel(dueDate, dueState)
  if (!label) return null
  const styles: Record<DueState, string> = {
    NONE: '',
    UPCOMING: 'text-muted-foreground',
    DUE_TODAY: 'bg-amber-50 text-amber-800 ring-1 ring-inset ring-amber-600/20',
    OVERDUE: 'bg-red-50 text-red-700 ring-1 ring-inset ring-red-600/20',
    COMPLETED: 'text-emerald-700',
  }
  const Icon = dueState === 'OVERDUE' ? AlertTriangle : dueState === 'COMPLETED' ? CheckCircle2 : CalendarDays
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-1.5 py-0.5 text-xs font-medium', styles[dueState], className)}>
      <Icon className="size-3.5" aria-hidden />
      {label}
    </span>
  )
}

export function TagBadge({ tag, className }: { tag: Pick<Tag, 'name' | 'color'>; className?: string }) {
  return (
    <span
      className={cn(
        'inline-flex max-w-full items-center truncate rounded-md px-1.5 py-0.5 text-[11px] font-medium ring-1 ring-inset',
        TAG_COLORS[tag.color] ?? TAG_COLORS.slate,
        className,
      )}
    >
      {tag.name}
    </span>
  )
}

export function StatusBadge({ status, className }: { status: TaskStatus; className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium', STATUS_BADGE[status], className)}>
      <span className={cn('size-1.5 rounded-full', STATUS_DOT[status])} aria-hidden />
      {STATUS_LABEL[status]}
    </span>
  )
}
