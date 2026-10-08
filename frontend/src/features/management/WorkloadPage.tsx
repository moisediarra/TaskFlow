import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { AlertTriangle, Info, Users } from 'lucide-react'
import { managementApi } from '@/api/endpoints'
import { PageHeader } from '@/components/common/PageHeader'
import { EmptyState, ErrorState, ListSkeleton } from '@/components/common/states'
import { UserAvatar } from '@/components/common/UserAvatar'
import { queryKeys } from '@/lib/query-keys'
import { cn } from '@/lib/utils'

/**
 * Active tasks per person (claude.md §26). It points at possible imbalances and bottlenecks; it is not a
 * performance judgement, so the wording stays neutral.
 */
export function WorkloadPage() {
  const workload = useQuery({ queryKey: queryKeys.management.workload, queryFn: managementApi.workload })

  return (
    <div>
      <PageHeader
        title="Team workload"
        description="Active tasks (To Do + In Progress) assigned to each project member."
      />
      {workload.isPending ? (
        <ListSkeleton rows={6} />
      ) : workload.isError ? (
        <ErrorState error={workload.error} onRetry={() => workload.refetch()} />
      ) : workload.data.rows.length === 0 ? (
        <EmptyState icon={<Users />} title="No project members yet" description="Workload appears once people join projects." />
      ) : (
        <div className="grid gap-4 lg:grid-cols-3">
          <section className="rounded-xl border bg-card p-5 shadow-xs lg:col-span-2" aria-label="Workload per person">
            <ul className="grid gap-4">
              {workload.data.rows.map((row) => {
                const share = Math.min(100, (row.active / workload.data.scaleMax) * 100)
                return (
                  <li key={row.userId}>
                    <div className="mb-1.5 flex items-center gap-2">
                      <UserAvatar name={row.name} id={row.userId} size="sm" />
                      <Link to={`/management/users/${row.userId}`} className="truncate text-sm font-medium hover:underline">
                        {row.name}
                      </Link>
                      {row.jobTitle ? <span className="hidden truncate text-xs text-muted-foreground sm:inline">{row.jobTitle}</span> : null}
                      <span className="ml-auto shrink-0 text-sm">
                        <span className="font-semibold">{row.active}</span>{' '}
                        <span className="text-muted-foreground">active {row.active === 1 ? 'task' : 'tasks'}</span>
                      </span>
                    </div>
                    <div
                      className="h-2.5 overflow-hidden rounded-full bg-teal-100"
                      role="meter"
                      aria-valuemin={0}
                      aria-valuemax={workload.data.scaleMax}
                      aria-valuenow={row.active}
                      aria-label={`${row.name}: ${row.active} active tasks`}
                    >
                      <div
                        className={cn('h-full rounded-full transition-[width]', row.manyActive ? 'bg-amber-500' : 'bg-teal-600')}
                        style={{ width: `${share}%` }}
                      />
                    </div>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {row.inProgress} in progress · {row.todo} to do
                      {row.overdue > 0 ? (
                        <span className={cn('ml-1', row.manyOverdue ? 'font-medium text-red-700' : '')}>· {row.overdue} overdue</span>
                      ) : null}
                    </p>
                  </li>
                )
              })}
            </ul>
          </section>

          <aside className="grid content-start gap-4">
            <section className="rounded-xl border bg-card p-5 shadow-xs">
              <h2 className="font-semibold">Things to look at</h2>
              {workload.data.warnings.length === 0 ? (
                <p className="mt-2 text-sm text-muted-foreground">No workload issues detected.</p>
              ) : (
                <ul className="mt-3 grid gap-2">
                  {workload.data.warnings.map((warning) => (
                    <li key={warning} className="flex items-start gap-2 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">
                      <AlertTriangle className="mt-0.5 size-4 shrink-0 text-amber-600" aria-label="Warning" />
                      {warning}
                    </li>
                  ))}
                </ul>
              )}
            </section>
            <p className="flex gap-2 rounded-xl border bg-card p-4 text-xs text-muted-foreground shadow-xs">
              <Info className="size-4 shrink-0" />
              Flags appear at {workload.data.activeTaskWarning} or more active tasks, or {workload.data.overdueTaskWarning} or
              more overdue tasks. They help spot imbalances and bottlenecks; they are not a measure of performance.
            </p>
          </aside>
        </div>
      )}
    </div>
  )
}
