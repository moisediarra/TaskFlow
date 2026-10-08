import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { FolderPlus, Plus } from 'lucide-react'
import { projectsApi } from '@/api/endpoints'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { PageHeader } from '@/components/common/PageHeader'
import { EmptyState, ErrorState } from '@/components/common/states'
import { useCurrentUser } from '@/features/auth/use-auth'
import { queryKeys } from '@/lib/query-keys'
import { ProjectCard } from './ProjectCard'
import { ProjectFormDialog } from './ProjectFormDialog'

export function ProjectsPage() {
  const user = useCurrentUser()
  const navigate = useNavigate()
  const [creating, setCreating] = useState(false)
  const projects = useQuery({ queryKey: queryKeys.projects, queryFn: projectsApi.list })
  const canCreate = user.role === 'PROJECT_OWNER' || user.role === 'IT_MANAGER'

  return (
    <div>
      <PageHeader
        title="Projects"
        description="Projects you own or take part in."
        actions={
          canCreate ? (
            <Button onClick={() => setCreating(true)}>
              <Plus /> New project
            </Button>
          ) : null
        }
      />
      {projects.isPending ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }, (_, index) => (
            <Skeleton key={index} className="h-44 rounded-xl" />
          ))}
        </div>
      ) : projects.isError ? (
        <ErrorState error={projects.error} onRetry={() => projects.refetch()} />
      ) : projects.data.length === 0 ? (
        canCreate ? (
          <EmptyState
            icon={<FolderPlus />}
            title="No projects yet."
            description="Create your first project and start organizing your tasks."
            action={
              <Button onClick={() => setCreating(true)}>
                <Plus /> Create Project
              </Button>
            }
          />
        ) : (
          <EmptyState
            icon={<FolderPlus />}
            title="You're not part of any project yet."
            description="When a project owner adds you to a project, it will appear here."
          />
        )
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.data.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </div>
      )}
      <ProjectFormDialog open={creating} onOpenChange={setCreating} onSaved={(project) => navigate(`/projects/${project.id}`)} />
    </div>
  )
}
