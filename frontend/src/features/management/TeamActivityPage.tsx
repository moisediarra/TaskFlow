import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'
import { Users } from 'lucide-react'
import { managementApi, type DueFilter, type TeamActivityFilters } from '@/api/endpoints'
import type { TaskPriority, TaskStatus } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { FilterSelect } from '@/components/common/FilterSelect'
import { PageHeader } from '@/components/common/PageHeader'
import { Pagination } from '@/components/common/Pagination'
import { EmptyState, ErrorState, ListSkeleton } from '@/components/common/states'
import { UserAvatar } from '@/components/common/UserAvatar'
import { DueBadge, PriorityBadge, StatusBadge } from '@/features/tasks/badges'
import { timeAgo } from '@/lib/format'
import { PRIORITIES, PRIORITY_LABEL, STATUSES, STATUS_LABEL } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'
import { useFilterOptions } from './use-filter-options'

const DUE_OPTIONS: { value: DueFilter; label: string }[] = [
  { value: 'OVERDUE', label: 'Overdue' },
  { value: 'TODAY', label: 'Due today' },
  { value: 'THIS_WEEK', label: 'Due within 7 days' },
  { value: 'NONE', label: 'No due date' },
]

/** "Who is doing what?" (claude.md §25), filterable by user, project, status, priority and due date. */
export function TeamActivityPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { userOptions, projectOptions } = useFilterOptions()
  const filters: TeamActivityFilters = {
    userId: searchParams.get('userId') ?? undefined,
    projectId: searchParams.get('projectId') ?? undefined,
    status: (searchParams.get('status') as TaskStatus | 'ALL' | null) ?? undefined,
    priority: (searchParams.get('priority') as TaskPriority | null) ?? undefined,
    due: (searchParams.get('due') as DueFilter | null) ?? undefined,
    page: Number(searchParams.get('page') ?? 0),
  }
  const activity = useQuery({
    queryKey: queryKeys.management.teamActivity(filters),
    queryFn: () => managementApi.teamActivity(filters),
    placeholderData: keepPreviousData,
  })

  const update = (key: string, value: string | undefined) =>
    setSearchParams((params) => {
      if (value) params.set(key, value)
      else params.delete(key)
      if (key !== 'page') params.delete('page')
      return params
    })
  const hasFilters = ['userId', 'projectId', 'status', 'priority', 'due'].some((key) => searchParams.has(key))

  return (
    <div>
      <PageHeader
        title="Team activity"
        description="What each person is working on. Shows To Do and In Progress unless you choose another status."
      />
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <FilterSelect id="ta-user" label="User" value={filters.userId} options={userOptions} anyLabel="Everyone" onChange={(v) => update('userId', v)} />
        <FilterSelect id="ta-project" label="Project" value={filters.projectId} options={projectOptions} anyLabel="All projects" onChange={(v) => update('projectId', v)} />
        <FilterSelect
          id="ta-status"
          label="Status"
          value={filters.status}
          anyLabel="Active (To Do + In Progress)"
          options={[...STATUSES.map((status) => ({ value: status, label: STATUS_LABEL[status] })), { value: 'ALL', label: 'All statuses' }]}
          onChange={(v) => update('status', v)}
        />
        <FilterSelect
          id="ta-priority"
          label="Priority"
          value={filters.priority}
          options={PRIORITIES.map((priority) => ({ value: priority, label: PRIORITY_LABEL[priority] }))}
          onChange={(v) => update('priority', v)}
        />
        <FilterSelect id="ta-due" label="Due date" value={filters.due} options={DUE_OPTIONS} onChange={(v) => update('due', v)} />
        {hasFilters ? (
          <Button variant="ghost" onClick={() => setSearchParams({})}>
            Clear filters
          </Button>
        ) : null}
      </div>

      {activity.isPending ? (
        <ListSkeleton rows={6} />
      ) : activity.isError ? (
        <ErrorState error={activity.error} onRetry={() => activity.refetch()} />
      ) : activity.data.items.length === 0 ? (
        <EmptyState
          icon={<Users />}
          title={hasFilters ? 'No tasks match these filters' : 'Nobody has active work right now'}
          description={hasFilters ? 'Try widening the filters.' : 'Assigned To Do and In Progress tasks will show up here.'}
        />
      ) : (
        <div className={activity.isPlaceholderData ? 'opacity-70 transition-opacity' : undefined}>
          <div className="overflow-x-auto rounded-xl border bg-card shadow-xs">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>User</TableHead>
                  <TableHead>Current task</TableHead>
                  <TableHead>Project</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Priority</TableHead>
                  <TableHead>Due</TableHead>
                  <TableHead>Last update</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {activity.data.items.map((item) => (
                  <TableRow key={item.taskId}>
                    <TableCell>
                      <span className="flex items-center gap-2 font-medium">
                        <UserAvatar name={item.assigneeName ?? '?'} id={item.assigneeId ?? undefined} size="sm" />
                        {item.assigneeName}
                      </span>
                    </TableCell>
                    <TableCell className="max-w-72">
                      <Link to={`/projects/${item.projectId}?task=${item.taskId}`} className="block truncate hover:text-primary hover:underline">
                        {item.title}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Link to={`/projects/${item.projectId}/overview`} className="hover:underline">
                        {item.projectName}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={item.status} />
                    </TableCell>
                    <TableCell>
                      <PriorityBadge priority={item.priority} />
                    </TableCell>
                    <TableCell>
                      <DueBadge dueDate={item.dueDate} dueState={item.dueState} />
                    </TableCell>
                    <TableCell className="text-muted-foreground">{timeAgo(item.updatedAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <Pagination
            page={activity.data.page}
            totalPages={activity.data.totalPages}
            totalItems={activity.data.totalItems}
            onPageChange={(page) => update('page', String(page))}
          />
        </div>
      )}
    </div>
  )
}
