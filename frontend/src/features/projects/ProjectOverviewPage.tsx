import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { History, Users } from 'lucide-react'
import { projectsApi } from '@/api/endpoints'
import type { StatusCounts } from '@/api/types'
import { Skeleton } from '@/components/ui/skeleton'
import { ActivityList } from '@/components/common/ActivityList'
import { EmptyState, ErrorState } from '@/components/common/states'
import { UserAvatar } from '@/components/common/UserAvatar'
import { DueBadge, StatusBadge } from '@/features/tasks/badges'
import { STATUS_DOT } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'
import { cn } from '@/lib/utils'
import { useProject } from './project-context'

/** Project monitoring (claude.md §30): counts, who is working on what, members and recent activity. */
export function ProjectOverviewPage() {
  const project = useProject()
  const overview = useQuery({ queryKey: queryKeys.overview(project.id), queryFn: () => projectsApi.overview(project.id) })
  const activity = useInfiniteQuery({
    queryKey: queryKeys.projectActivity(project.id),
    queryFn: ({ pageParam }) => projectsApi.activity(project.id, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  })

  if (overview.isPending) {
    return (
      <div className="grid gap-4 lg:grid-cols-3">
        <Skeleton className="h-72 rounded-xl" />
        <Skeleton className="h-72 rounded-xl lg:col-span-2" />
      </div>
    )
  }
  if (overview.isError) return <ErrorState error={overview.error} onRetry={() => overview.refetch()} />
  const data = overview.data
  const activities = activity.data?.pages.flatMap((page) => page.items) ?? []

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <section className="rounded-xl border bg-card p-5 shadow-xs">
        <h2 className="flex items-center justify-between font-semibold">
          Tasks
          <span className="flex items-center gap-1.5 text-sm font-normal text-muted-foreground">
            <Users className="size-4" /> Members: {data.memberCount}
          </span>
        </h2>
        <TaskCounts counts={data.tasks} />
        <div className="mt-3 grid grid-cols-2 gap-2 border-t pt-3 text-sm">
          <div className={cn('rounded-lg px-3 py-2', data.overdue > 0 ? 'bg-red-50 text-red-700' : 'bg-muted')}>
            <p className="text-xs">Overdue</p>
            <p className="text-xl font-semibold tabular-nums">{data.overdue}</p>
          </div>
          <div className={cn('rounded-lg px-3 py-2', data.highPriority > 0 ? 'bg-amber-50 text-amber-800' : 'bg-muted')}>
            <p className="text-xs">High priority</p>
            <p className="text-xl font-semibold tabular-nums">{data.highPriority}</p>
          </div>
        </div>
      </section>

      <section className="rounded-xl border bg-card p-5 shadow-xs lg:col-span-2">
        <h2 className="font-semibold">Current team activity</h2>
        <p className="text-sm text-muted-foreground">Who is working on what in this project.</p>
        {data.currentActivity.length === 0 ? (
          <EmptyState className="mt-4" title="Nobody is assigned yet" description="Assign tasks on the board to see who is doing what." />
        ) : (
          <ul className="mt-3 divide-y">
            {data.currentActivity.map((item) => (
              <li key={item.taskId}>
                <Link
                  to={`/projects/${project.id}?task=${item.taskId}`}
                  className="flex flex-wrap items-center gap-x-3 gap-y-1 py-2.5 hover:bg-muted/50 sm:flex-nowrap"
                >
                  <span className="flex w-40 shrink-0 items-center gap-2 text-sm font-medium">
                    <UserAvatar name={item.assigneeName} id={item.assigneeId} size="sm" />
                    <span className="truncate">{item.assigneeName}</span>
                  </span>
                  <span className="text-muted-foreground" aria-hidden>
                    →
                  </span>
                  <span className="min-w-0 flex-1 truncate text-sm">{item.title}</span>
                  <DueBadge dueDate={item.dueDate} dueState={item.dueState} />
                  <StatusBadge status={item.status} />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="rounded-xl border bg-card p-5 shadow-xs">
        <h2 className="font-semibold">Members</h2>
        <ul className="mt-3 grid gap-3">
          {data.members.map((member) => (
            <li key={member.userId} className="flex items-center gap-3">
              <UserAvatar name={member.name} id={member.userId} />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium">
                  {member.name}
                  {!member.active ? <span className="font-normal text-muted-foreground"> (deactivated)</span> : null}
                </p>
                <p className="truncate text-xs text-muted-foreground">{member.jobTitle ?? member.email}</p>
              </div>
              {member.role === 'OWNER' ? (
                <span className="rounded-full bg-accent px-2 py-0.5 text-[11px] font-medium text-accent-foreground">Owner</span>
              ) : null}
            </li>
          ))}
        </ul>
      </section>

      <section className="rounded-xl border bg-card p-5 shadow-xs lg:col-span-2">
        <h2 className="flex items-center gap-2 font-semibold">
          <History className="size-4" /> Activity
        </h2>
        {activity.isPending ? (
          <Skeleton className="mt-3 h-40" />
        ) : activity.isError ? (
          <ErrorState error={activity.error} onRetry={() => activity.refetch()} className="mt-3" />
        ) : activities.length === 0 ? (
          <p className="mt-3 text-sm text-muted-foreground">No activity yet.</p>
        ) : (
          <ActivityList
            className="mt-2"
            items={activities}
            hasMore={activity.hasNextPage}
            loadingMore={activity.isFetchingNextPage}
            onLoadMore={() => activity.fetchNextPage()}
          />
        )}
      </section>
    </div>
  )
}

function TaskCounts({ counts }: { counts: StatusCounts }) {
  const rows = [
    { label: 'Backlog', value: counts.backlog, dot: STATUS_DOT.BACKLOG },
    { label: 'To Do', value: counts.todo, dot: STATUS_DOT.TODO },
    { label: 'In Progress', value: counts.inProgress, dot: STATUS_DOT.IN_PROGRESS },
    { label: 'Done', value: counts.done, dot: STATUS_DOT.DONE },
  ]
  return (
    <dl className="mt-3 grid gap-2">
      {rows.map((row) => (
        <div key={row.label} className="flex items-center justify-between text-sm">
          <dt className="flex items-center gap-2 text-muted-foreground">
            <span className={cn('size-2 rounded-full', row.dot)} aria-hidden />
            {row.label}
          </dt>
          <dd className="font-semibold tabular-nums">{row.value}</dd>
        </div>
      ))}
    </dl>
  )
}
