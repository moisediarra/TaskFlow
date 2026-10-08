import type { TaskCard } from '@/api/types'
import { UserAvatar } from '@/components/common/UserAvatar'
import { DueBadge, PriorityDot, TagBadge } from '@/features/tasks/badges'
import { cn } from '@/lib/utils'

/** Visual card: priority, title, tags, deadline and assignee (claude.md §16–18). */
export function TaskCardView({ task, overlay = false, dragging = false }: { task: TaskCard; overlay?: boolean; dragging?: boolean }) {
  const extraTags = task.tags.length - 3
  return (
    <div
      className={cn(
        'rounded-lg border bg-card p-3 text-left shadow-xs transition-shadow',
        'group-hover/card:border-primary/30 group-hover/card:shadow-md group-focus-visible/card:ring-3 group-focus-visible/card:ring-ring/50',
        task.dueState === 'OVERDUE' && 'border-l-4 border-l-red-500',
        overlay && 'rotate-2 cursor-grabbing shadow-xl',
        dragging && 'opacity-40',
      )}
    >
      <div className="flex items-start gap-2">
        <span className="mt-0.5">
          <PriorityDot priority={task.priority} />
        </span>
        <p className="min-w-0 flex-1 text-sm font-medium leading-snug break-words line-clamp-3">{task.title}</p>
      </div>
      {task.tags.length > 0 ? (
        <div className="mt-2 flex flex-wrap gap-1">
          {task.tags.slice(0, 3).map((tag) => (
            <TagBadge key={tag.id} tag={tag} />
          ))}
          {extraTags > 0 ? <span className="px-1 text-[11px] text-muted-foreground">+{extraTags}</span> : null}
        </div>
      ) : null}
      <div className="mt-2.5 flex min-h-6 items-center justify-between gap-2">
        <DueBadge dueDate={task.dueDate} dueState={task.dueState} className="-ml-1.5" />
        {task.assignee ? (
          <UserAvatar name={task.assignee.name} id={task.assignee.id} size="sm" className="ml-auto" />
        ) : (
          <span className="ml-auto text-[11px] text-muted-foreground">Unassigned</span>
        )}
      </div>
    </div>
  )
}
