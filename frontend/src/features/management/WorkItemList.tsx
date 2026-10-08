import { Link } from 'react-router'
import type { WorkItem } from '@/api/types'
import { UserAvatar } from '@/components/common/UserAvatar'
import { DueBadge, PriorityDot, StatusBadge } from '@/features/tasks/badges'

/** "Who is doing what" rows: person → task → project. */
export function WorkItemList({ items, empty }: { items: WorkItem[]; empty: string }) {
  if (items.length === 0) return <p className="py-6 text-center text-sm text-muted-foreground">{empty}</p>
  return (
    <ul className="divide-y">
      {items.map((item) => (
        <li key={item.taskId}>
          <Link
            to={`/projects/${item.projectId}?task=${item.taskId}`}
            className="-mx-2 flex items-center gap-3 rounded-md px-2 py-2.5 hover:bg-muted/60"
          >
            {item.assigneeName ? (
              <UserAvatar name={item.assigneeName} id={item.assigneeId ?? undefined} size="sm" />
            ) : (
              <span className="size-6 shrink-0 rounded-full border border-dashed" title="Unassigned" />
            )}
            <span className="min-w-0 flex-1">
              <span className="flex items-center gap-1.5">
                <PriorityDot priority={item.priority} />
                <span className="truncate text-sm font-medium">{item.title}</span>
              </span>
              <span className="mt-0.5 block truncate text-xs text-muted-foreground">
                {item.assigneeName ?? 'Unassigned'} · {item.projectName}
              </span>
            </span>
            <span className="hidden shrink-0 items-center gap-2 sm:flex">
              <DueBadge dueDate={item.dueDate} dueState={item.dueState} />
              <StatusBadge status={item.status} />
            </span>
          </Link>
        </li>
      ))}
    </ul>
  )
}
