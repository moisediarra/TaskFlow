import { useQuery } from '@tanstack/react-query'
import { Link, NavLink, Outlet, useOutletContext, useParams } from 'react-router'
import { ChevronLeft, Eye } from 'lucide-react'
import { projectsApi } from '@/api/endpoints'
import type { ProjectDetail } from '@/api/types'
import { Skeleton } from '@/components/ui/skeleton'
import { ErrorState } from '@/components/common/states'
import { queryKeys } from '@/lib/query-keys'
import { cn } from '@/lib/utils'

export interface ProjectOutletContext {
  project: ProjectDetail
}

export function useProject() {
  return useOutletContext<ProjectOutletContext>().project
}

/** Header and tabs (Board · Overview · Settings) shared by a project's pages. */
export function ProjectLayout() {
  const { projectId = '' } = useParams()
  const project = useQuery({ queryKey: queryKeys.project(projectId), queryFn: () => projectsApi.get(projectId) })

  if (project.isPending) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-4 w-96 max-w-full" />
        <Skeleton className="mt-6 h-96 w-full rounded-xl" />
      </div>
    )
  }
  if (project.isError) return <ErrorState error={project.error} onRetry={() => project.refetch()} className="mt-10" />

  const data = project.data
  const tabs = [
    { to: `/projects/${data.id}`, label: 'Board', end: true },
    { to: `/projects/${data.id}/overview`, label: 'Overview' },
    ...(data.permissions.canManage ? [{ to: `/projects/${data.id}/settings`, label: 'Settings' }] : []),
  ]

  return (
    <div>
      <Link to="/projects" className="mb-2 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ChevronLeft className="size-4" /> Projects
      </Link>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h1 className="text-2xl font-semibold tracking-tight">{data.name}</h1>
          {data.description ? <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{data.description}</p> : null}
        </div>
        {data.myRole === null ? (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground">
            <Eye className="size-3.5" /> Viewing as IT Manager (read-only)
          </span>
        ) : null}
      </div>
      <nav aria-label="Project" className="mt-4 mb-5 flex gap-1 border-b">
        {tabs.map(({ to, label, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              cn(
                '-mb-px border-b-2 border-transparent px-3 py-2 text-sm font-medium text-muted-foreground hover:text-foreground',
                isActive && 'border-primary text-foreground',
              )
            }
          >
            {label}
          </NavLink>
        ))}
      </nav>
      <Outlet context={{ project: data } satisfies ProjectOutletContext} />
    </div>
  )
}
