import { useQuery } from '@tanstack/react-query'
import { managementApi } from '@/api/endpoints'
import { queryKeys } from '@/lib/query-keys'

/** People and projects for the filter dropdowns of the management pages. */
export function useFilterOptions() {
  const people = useQuery({
    queryKey: queryKeys.management.users({ options: true }),
    queryFn: () => managementApi.users({ size: 100 }),
    staleTime: 60_000,
  })
  const projects = useQuery({
    queryKey: queryKeys.management.projects({ options: true }),
    queryFn: () => managementApi.projects({ size: 100 }),
    staleTime: 60_000,
  })
  return {
    userOptions: (people.data?.items ?? []).map((user) => ({ value: user.id, label: user.name })),
    projectOptions: (projects.data?.items ?? []).map((project) => ({ value: project.id, label: project.name })),
  }
}
