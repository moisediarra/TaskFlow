import {
  ArrowRightLeft,
  CalendarClock,
  CheckCircle2,
  Flag,
  FolderPlus,
  FolderX,
  Loader2,
  Pencil,
  ShieldCheck,
  Trash2,
  UserMinus,
  UserPlus,
  type LucideIcon,
} from 'lucide-react'
import type { Activity, ActivityAction } from '@/api/types'
import { Button } from '@/components/ui/button'
import { formatDateTime, formatTime, timeAgo } from '@/lib/format'
import { cn } from '@/lib/utils'

const ICONS: Record<ActivityAction, { icon: LucideIcon; tone: string }> = {
  PROJECT_CREATED: { icon: FolderPlus, tone: 'bg-teal-50 text-teal-700' },
  PROJECT_UPDATED: { icon: Pencil, tone: 'bg-slate-100 text-slate-600' },
  PROJECT_DELETED: { icon: FolderX, tone: 'bg-red-50 text-red-600' },
  PROJECT_MEMBER_ADDED: { icon: UserPlus, tone: 'bg-sky-50 text-sky-700' },
  PROJECT_MEMBER_REMOVED: { icon: UserMinus, tone: 'bg-slate-100 text-slate-600' },
  TASK_CREATED: { icon: FolderPlus, tone: 'bg-teal-50 text-teal-700' },
  TASK_UPDATED: { icon: Pencil, tone: 'bg-slate-100 text-slate-600' },
  TASK_DELETED: { icon: Trash2, tone: 'bg-red-50 text-red-600' },
  TASK_ASSIGNED: { icon: UserPlus, tone: 'bg-sky-50 text-sky-700' },
  TASK_UNASSIGNED: { icon: UserMinus, tone: 'bg-slate-100 text-slate-600' },
  TASK_STATUS_CHANGED: { icon: ArrowRightLeft, tone: 'bg-amber-50 text-amber-700' },
  TASK_PRIORITY_CHANGED: { icon: Flag, tone: 'bg-rose-50 text-rose-700' },
  TASK_DUE_DATE_CHANGED: { icon: CalendarClock, tone: 'bg-violet-50 text-violet-700' },
  TASK_COMPLETED: { icon: CheckCircle2, tone: 'bg-emerald-50 text-emerald-700' },
  USER_ROLE_CHANGED: { icon: ShieldCheck, tone: 'bg-indigo-50 text-indigo-700' },
  USER_STATUS_CHANGED: { icon: ShieldCheck, tone: 'bg-indigo-50 text-indigo-700' },
}

interface ActivityListProps {
  items: Activity[]
  showProject?: boolean
  hasMore?: boolean
  loadingMore?: boolean
  onLoadMore?: () => void
  className?: string
}

/** Chronological list of business actions (claude.md §27). */
export function ActivityList({ items, showProject = false, hasMore, loadingMore, onLoadMore, className }: ActivityListProps) {
  return (
    <div className={className}>
      <ol className="grid gap-1">
        {items.map((activity) => {
          const { icon: Icon, tone } = ICONS[activity.action] ?? ICONS.TASK_UPDATED
          return (
            <li key={activity.id} className="flex gap-3 rounded-lg px-2 py-2">
              <span className={cn('mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full', tone)} aria-hidden>
                <Icon className="size-3.5" />
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-sm leading-snug">{activity.description}</p>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  <time dateTime={activity.createdAt} title={formatDateTime(activity.createdAt)}>
                    {formatTime(activity.createdAt)} · {timeAgo(activity.createdAt)}
                  </time>
                  {showProject && activity.projectName ? <> · {activity.projectName}</> : null}
                </p>
              </div>
            </li>
          )
        })}
      </ol>
      {hasMore && onLoadMore ? (
        <Button variant="ghost" size="sm" className="mt-2 w-full" disabled={loadingMore} onClick={onLoadMore}>
          {loadingMore ? <Loader2 className="animate-spin" /> : null}
          Load more
        </Button>
      ) : null}
    </div>
  )
}
