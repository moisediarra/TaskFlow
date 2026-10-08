import { useState } from 'react'
import { keepPreviousData, useInfiniteQuery } from '@tanstack/react-query'
import { History } from 'lucide-react'
import { managementApi, type ActivityLogFilters } from '@/api/endpoints'
import type { ActivityAction } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ActivityList } from '@/components/common/ActivityList'
import { FilterSelect } from '@/components/common/FilterSelect'
import { PageHeader } from '@/components/common/PageHeader'
import { EmptyState, ErrorState, ListSkeleton } from '@/components/common/states'
import { ACTIVITY_ACTIONS, ACTIVITY_LABEL } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'
import { useDebouncedValue } from '@/lib/use-debounced-value'
import { useFilterOptions } from './use-filter-options'

/** Global activity history (claude.md §27), cursor-paginated with filters. */
export function ActivityLogsPage() {
  const { userOptions, projectOptions } = useFilterOptions()
  const [text, setText] = useState('')
  const [filters, setFilters] = useState<Omit<ActivityLogFilters, 'q'>>({})
  const q = useDebouncedValue(text.trim(), 300)
  const active: ActivityLogFilters = { ...filters, q: q || undefined }

  const logs = useInfiniteQuery({
    queryKey: queryKeys.management.activityLogs(active),
    queryFn: ({ pageParam }) => managementApi.activityLogs(active, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    placeholderData: keepPreviousData,
  })
  const items = logs.data?.pages.flatMap((page) => page.items) ?? []
  const set = (key: keyof typeof filters, value: string | undefined) => setFilters((current) => ({ ...current, [key]: value }))
  const hasFilters = Object.values(filters).some(Boolean) || text !== ''

  return (
    <div>
      <PageHeader title="Activity logs" description="Meaningful business actions across all projects. Page views and clicks are never recorded." />
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="grid min-w-52 flex-1 gap-1 sm:max-w-xs">
          <Label htmlFor="log-search" className="text-xs text-muted-foreground">
            Search
          </Label>
          <Input id="log-search" placeholder="Search descriptions…" value={text} onChange={(event) => setText(event.target.value)} className="bg-card" />
        </div>
        <FilterSelect id="log-user" label="User" value={filters.userId} options={userOptions} anyLabel="Everyone" onChange={(v) => set('userId', v)} />
        <FilterSelect id="log-project" label="Project" value={filters.projectId} options={projectOptions} anyLabel="All projects" onChange={(v) => set('projectId', v)} />
        <FilterSelect
          id="log-action"
          label="Action"
          value={filters.action}
          options={ACTIVITY_ACTIONS.map((action) => ({ value: action, label: ACTIVITY_LABEL[action] }))}
          anyLabel="All actions"
          onChange={(v) => set('action', v as ActivityAction | undefined)}
        />
        <div className="grid gap-1">
          <Label htmlFor="log-from" className="text-xs text-muted-foreground">
            From
          </Label>
          <Input id="log-from" type="date" value={filters.from ?? ''} onChange={(event) => set('from', event.target.value || undefined)} className="bg-card" />
        </div>
        <div className="grid gap-1">
          <Label htmlFor="log-to" className="text-xs text-muted-foreground">
            To
          </Label>
          <Input id="log-to" type="date" value={filters.to ?? ''} onChange={(event) => set('to', event.target.value || undefined)} className="bg-card" />
        </div>
        {hasFilters ? (
          <Button
            variant="ghost"
            onClick={() => {
              setFilters({})
              setText('')
            }}
          >
            Clear filters
          </Button>
        ) : null}
      </div>

      {logs.isPending ? (
        <ListSkeleton rows={8} />
      ) : logs.isError ? (
        <ErrorState error={logs.error} onRetry={() => logs.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState icon={<History />} title="No activity found" description={hasFilters ? 'Try widening the filters.' : 'Actions will appear here as people work.'} />
      ) : (
        <div className={`rounded-xl border bg-card p-3 shadow-xs ${logs.isPlaceholderData ? 'opacity-70' : ''}`}>
          <ActivityList
            items={items}
            showProject
            hasMore={logs.hasNextPage}
            loadingMore={logs.isFetchingNextPage}
            onLoadMore={() => logs.fetchNextPage()}
          />
        </div>
      )}
    </div>
  )
}
