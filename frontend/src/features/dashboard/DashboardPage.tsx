import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { AlertTriangle, CalendarClock, CircleCheckBig, Flame, FolderKanban, ListTodo, Timer } from 'lucide-react'
import { dashboardApi } from '@/api/endpoints'
import type { TaskList, TaskRef } from '@/api/types'
import { Skeleton } from '@/components/ui/skeleton'
import { PageHeader } from '@/components/common/PageHeader'
import { StatTile } from '@/components/common/StatTile'
import { EmptyState, ErrorState } from '@/components/common/states'
import { useCurrentUser } from '@/features/auth/use-auth'
import { ProjectCard } from '@/features/projects/ProjectCard'
import { DueBadge, PriorityDot, StatusBadge } from '@/features/tasks/badges'
import { greeting } from '@/lib/format'
import { queryKeys } from '@/lib/query-keys'

/** "My Tasks" dashboard (claude.md §6). */
export function DashboardPage() {
  const user = useCurrentUser()
  const dashboard = useQuery({ queryKey: queryKeys.dashboard, queryFn: dashboardApi.get })
  const firstName = user.name.split(' ')[0]

  return (
    <div>
      <PageHeader title={`${greeting()}, ${firstName} 👋`} description="Here is what's on your plate." />
      {dashboard.isPending ? (
        <div className="grid gap-4">
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            {Array.from({ length: 4 }, (_, index) => (
              <Skeleton key={index} className="h-28 rounded-xl" />
            ))}
          </div>
          <Skeleton className="h-56 rounded-xl" />
        </div>
      ) : dashboard.isError ? (
        <ErrorState error={dashboard.error} onRetry={() => dashboard.refetch()} />
      ) : (
        <div className="grid gap-6">
          <section aria-labelledby="my-tasks">
            <h2 id="my-tasks" className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
              My tasks
            </h2>
            <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              <StatTile
                label="Total tasks"
                value={dashboard.data.myTasks.total}
                icon={<ListTodo />}
                hint={dashboard.data.myTasks.backlog > 0 ? `incl. ${dashboard.data.myTasks.backlog} in Backlog` : 'Assigned to you'}
              />
              <StatTile label="In progress" value={dashboard.data.myTasks.inProgress} icon={<Timer />} />
              <StatTile label="To do" value={dashboard.data.myTasks.todo} icon={<CalendarClock />} />
              <StatTile label="Completed" value={dashboard.data.myTasks.done} icon={<CircleCheckBig />} tone="success" />
            </div>
          </section>

          <div className="grid gap-4 lg:grid-cols-3">
            <TaskListCard title="High priority" icon={<Flame className="text-red-600" />} list={dashboard.data.highPriority} empty="No high-priority work open." />
            <TaskListCard title="Due today" icon={<CalendarClock className="text-amber-600" />} list={dashboard.data.dueToday} empty="Nothing due today." />
            <TaskListCard title="Overdue" icon={<AlertTriangle className="text-red-600" />} list={dashboard.data.overdue} empty="Nothing overdue. Nice!" />
          </div>

          <section aria-labelledby="recent-projects">
            <div className="mb-3 flex items-center justify-between">
              <h2 id="recent-projects" className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                Recent projects
              </h2>
              <Link to="/projects" className="text-sm text-primary hover:underline">
                All projects
              </Link>
            </div>
            {dashboard.data.recentProjects.length === 0 ? (
              <EmptyState
                icon={<FolderKanban />}
                title="No projects yet."
                description={
                  user.role === 'MEMBER'
                    ? 'When a project owner adds you to a project, it will appear here.'
                    : 'Create your first project and start organizing your tasks.'
                }
              />
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {dashboard.data.recentProjects.map((project) => (
                  <ProjectCard key={project.id} project={project} />
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  )
}

function TaskListCard({ title, icon, list, empty }: { title: string; icon: ReactNode; list: TaskList; empty: string }) {
  return (
    <section className="rounded-xl border bg-card p-4 shadow-xs">
      <h2 className="flex items-center gap-2 font-semibold [&_svg]:size-4">
        {icon}
        {title}
        <span className="ml-auto rounded-full bg-muted px-2 text-xs font-medium tabular-nums text-muted-foreground">{list.total}</span>
      </h2>
      {list.items.length === 0 ? (
        <p className="mt-3 text-sm text-muted-foreground">{empty}</p>
      ) : (
        <ul className="mt-2 divide-y">
          {list.items.map((task) => (
            <TaskRow key={task.id} task={task} />
          ))}
        </ul>
      )}
      {list.total > list.items.length ? (
        <p className="mt-2 text-xs text-muted-foreground">+{list.total - list.items.length} more</p>
      ) : null}
    </section>
  )
}

function TaskRow({ task }: { task: TaskRef }) {
  return (
    <li>
      <Link to={`/projects/${task.projectId}?task=${task.id}`} className="-mx-2 flex items-start gap-2 rounded-md px-2 py-2 hover:bg-muted/60">
        <span className="mt-0.5">
          <PriorityDot priority={task.priority} />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium">{task.title}</span>
          <span className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
            {task.projectName}
            <StatusBadge status={task.status} />
            <DueBadge dueDate={task.dueDate} dueState={task.dueState} />
          </span>
        </span>
      </Link>
    </li>
  )
}
