import { Link } from 'react-router'
import { Clock, ListChecks, Users } from 'lucide-react'
import type { ProjectSummary } from '@/api/types'
import { pluralize, timeAgo } from '@/lib/format'

/** Project card (claude.md §7): name, active tasks, members, last update. */
export function ProjectCard({ project, to }: { project: ProjectSummary; to?: string }) {
  return (
    <Link
      to={to ?? `/projects/${project.id}`}
      className="group flex flex-col rounded-xl border bg-card p-4 shadow-xs transition-all hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-md focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <div className="flex items-start justify-between gap-2">
        <h3 className="font-semibold leading-snug group-hover:text-primary">{project.name}</h3>
        {project.myRole === 'OWNER' ? (
          <span className="shrink-0 rounded-full bg-accent px-2 py-0.5 text-[11px] font-medium text-accent-foreground">Owner</span>
        ) : null}
      </div>
      {project.description ? (
        <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{project.description}</p>
      ) : (
        <p className="mt-1 text-sm text-muted-foreground/70">Owned by {project.owner.name}</p>
      )}
      <div className="mt-4 grid gap-1.5 text-sm text-muted-foreground">
        <span className="flex items-center gap-2">
          <ListChecks className="size-4" aria-hidden />
          <span>
            <span className="font-medium text-foreground">{project.activeTaskCount}</span>{' '}
            {project.activeTaskCount === 1 ? 'active task' : 'active tasks'}
          </span>
        </span>
        <span className="flex items-center gap-2">
          <Users className="size-4" aria-hidden />
          {pluralize(project.memberCount, 'member')}
        </span>
      </div>
      <p className="mt-4 flex items-center gap-1.5 border-t pt-3 text-xs text-muted-foreground">
        <Clock className="size-3.5" aria-hidden />
        Updated {timeAgo(project.updatedAt)}
      </p>
    </Link>
  )
}
