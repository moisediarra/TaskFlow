import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { AlertTriangle, ArrowRight, Flame, FolderKanban, ListChecks, Timer, Users } from 'lucide-react'
import { managementApi } from '@/api/endpoints'
import { Skeleton } from '@/components/ui/skeleton'
import { ActivityList } from '@/components/common/ActivityList'
import { PageHeader } from '@/components/common/PageHeader'
import { StatTile } from '@/components/common/StatTile'
import { ErrorState } from '@/components/common/states'
import { PRIORITY_EMOJI, PRIORITY_LABEL } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'
import { HorizontalBars, PipelineBar } from './charts'
import { WorkItemList } from './WorkItemList'

/**
 * IT Management Dashboard (claude.md §24): who is doing what, what is in progress, what is overdue, who is
 * overloaded and what changed recently — on one screen.
 */
export function ManagementOverviewPage() {
  const overview = useQuery({ queryKey: queryKeys.management.overview, queryFn: managementApi.overview })

  if (overview.isPending) {
    return (
      <div className="grid gap-4">
        <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
          {Array.from({ length: 5 }, (_, index) => (
            <Skeleton key={index} className="h-28 rounded-xl" />
          ))}
        </div>
        <Skeleton className="h-72 rounded-xl" />
      </div>
    )
  }
  if (overview.isError) return <ErrorState error={overview.error} onRetry={() => overview.refetch()} />
  const { users, projects, tasks, openByPriority, inProgressNow, overdueTasks, topWorkload, workloadWarnings, recentActivity } =
    overview.data

  return (
    <div className={overview.isFetching ? 'opacity-80 transition-opacity' : undefined}>
      <PageHeader title="IT Management" description="Who is doing what, and where attention is needed." />

      <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
        <StatTile label="Users" value={users.total} icon={<Users />} hint={`${users.active} active`} />
        <StatTile label="Projects" value={projects.total} icon={<FolderKanban />} hint={`${projects.active} active`} />
        <StatTile label="Active tasks" value={tasks.active} icon={<ListChecks />} hint="To Do + In Progress" />
        <StatTile label="In progress" value={tasks.inProgress} icon={<Timer />} />
        <StatTile
          label="Overdue"
          value={tasks.overdue}
          icon={<AlertTriangle />}
          tone={tasks.overdue > 0 ? 'danger' : 'default'}
          hint={tasks.overdue > 0 ? 'Past their due date' : 'Nothing late'}
        />
      </div>
      <div className="mt-3 grid grid-cols-2 gap-3 md:grid-cols-5">
        <StatTile label="Total tasks" value={tasks.total} />
        <StatTile label="Backlog" value={tasks.backlog} />
        <StatTile label="To do" value={tasks.todo} />
        <StatTile label="Completed" value={tasks.done} tone="success" />
        <StatTile label="High priority" value={tasks.highPriority} icon={<Flame />} hint="Open, high priority" />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <Card title="What's in progress" action={<SeeAll to="/management/team-activity" />}>
          <WorkItemList items={inProgressNow} empty="Nothing is in progress right now." />
        </Card>
        <Card title="Overdue" action={<SeeAll to="/management/team-activity?due=OVERDUE" />}>
          <WorkItemList items={overdueTasks} empty="No overdue tasks." />
        </Card>

        <Card title="Who has the most active work" action={<SeeAll to="/management/workload" />}>
          {workloadWarnings.length > 0 ? (
            <ul className="mb-3 grid gap-1.5">
              {workloadWarnings.map((warning) => (
                <li key={warning} className="flex items-start gap-2 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">
                  <AlertTriangle className="mt-0.5 size-4 shrink-0 text-amber-600" aria-label="Warning" />
                  {warning}
                </li>
              ))}
            </ul>
          ) : null}
          {topWorkload.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">No project members yet.</p>
          ) : (
            <HorizontalBars
              data={topWorkload.map((row) => ({ name: row.name, value: row.active }))}
              valueLabel="active tasks"
            />
          )}
        </Card>

        <Card title="Work pipeline">
          <PipelineBar
            segments={[
              { label: 'Backlog', value: tasks.backlog },
              { label: 'To Do', value: tasks.todo },
              { label: 'In Progress', value: tasks.inProgress },
              { label: 'Done', value: tasks.done },
            ]}
          />
          <h3 className="mt-6 mb-2 text-sm font-medium">Open tasks by priority</h3>
          <HorizontalBars
            data={openByPriority.map((entry) => ({
              name: `${PRIORITY_EMOJI[entry.priority]} ${PRIORITY_LABEL[entry.priority]}`,
              value: entry.count,
            }))}
            valueLabel="open tasks"
            categoryWidth={90}
          />
        </Card>

        <Card title="Recent activity" action={<SeeAll to="/management/activity-logs" />} className="lg:col-span-2">
          {recentActivity.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">No activity yet.</p>
          ) : (
            <ActivityList items={recentActivity} showProject />
          )}
        </Card>
      </div>
    </div>
  )
}

function Card({ title, action, children, className }: { title: string; action?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`rounded-xl border bg-card p-5 shadow-xs ${className ?? ''}`}>
      <div className="mb-3 flex items-center justify-between gap-2">
        <h2 className="font-semibold">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}

function SeeAll({ to }: { to: string }) {
  return (
    <Link to={to} className="inline-flex items-center gap-1 text-sm text-primary hover:underline">
      See all <ArrowRight className="size-3.5" />
    </Link>
  )
}
