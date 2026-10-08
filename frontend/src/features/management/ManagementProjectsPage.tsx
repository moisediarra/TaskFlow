import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { FolderSearch } from 'lucide-react'
import { managementApi } from '@/api/endpoints'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { PageHeader } from '@/components/common/PageHeader'
import { Pagination } from '@/components/common/Pagination'
import { EmptyState, ErrorState } from '@/components/common/states'
import { ProjectCard } from '@/features/projects/ProjectCard'
import { queryKeys } from '@/lib/query-keys'
import { useDebouncedValue } from '@/lib/use-debounced-value'

/** Every project in the organization, for project monitoring (claude.md §30). */
export function ManagementProjectsPage() {
  const [text, setText] = useState('')
  const [page, setPage] = useState(0)
  const q = useDebouncedValue(text.trim(), 300)
  const filters = { q: q || undefined, page }
  const projects = useQuery({
    queryKey: queryKeys.management.projects(filters),
    queryFn: () => managementApi.projects(filters),
    placeholderData: keepPreviousData,
  })

  return (
    <div>
      <PageHeader title="All projects" description="Open any project to see its board, members and activity." />
      <div className="mb-4 grid max-w-xs gap-1">
        <Label htmlFor="project-search" className="text-xs text-muted-foreground">
          Search
        </Label>
        <Input
          id="project-search"
          placeholder="Project name…"
          value={text}
          onChange={(event) => {
            setText(event.target.value)
            setPage(0)
          }}
          className="bg-card"
        />
      </div>
      {projects.isPending ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, index) => (
            <Skeleton key={index} className="h-44 rounded-xl" />
          ))}
        </div>
      ) : projects.isError ? (
        <ErrorState error={projects.error} onRetry={() => projects.refetch()} />
      ) : projects.data.items.length === 0 ? (
        <EmptyState icon={<FolderSearch />} title="No projects found" description={q ? 'Try another name.' : 'Projects appear here once people create them.'} />
      ) : (
        <div className={projects.isPlaceholderData ? 'opacity-70 transition-opacity' : undefined}>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {projects.data.items.map((project) => (
              <ProjectCard key={project.id} project={project} to={`/projects/${project.id}/overview`} />
            ))}
          </div>
          <Pagination
            page={projects.data.page}
            totalPages={projects.data.totalPages}
            totalItems={projects.data.totalItems}
            onPageChange={setPage}
          />
        </div>
      )}
    </div>
  )
}
