import { useMemo, useState } from 'react'
import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'
import { Loader2, Plus } from 'lucide-react'
import { tasksApi } from '@/api/endpoints'
import type { TaskStatus } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { ErrorState } from '@/components/common/states'
import { UserAvatar } from '@/components/common/UserAvatar'
import { useProject } from '@/features/projects/project-context'
import { TaskFormDialog } from '@/features/tasks/TaskFormDialog'
import { TaskSheet } from '@/features/tasks/TaskSheet'
import { queryKeys } from '@/lib/query-keys'
import { KanbanBoard } from './KanbanBoard'

const DONE_PAGE_SIZE = 50

export function BoardPage() {
  const project = useProject()
  const [searchParams, setSearchParams] = useSearchParams()
  const [createIn, setCreateIn] = useState<TaskStatus | null>(null)
  const [showMoreDone, setShowMoreDone] = useState(false)
  const openTaskId = searchParams.get('task')

  const board = useQuery({ queryKey: queryKeys.board(project.id), queryFn: () => tasksApi.board(project.id) })

  // Done grows forever: the board ships its first 50 cards, older ones load on demand.
  const moreDone = useInfiniteQuery({
    queryKey: queryKeys.doneColumn(project.id),
    queryFn: ({ pageParam }) => tasksApi.column(project.id, 'DONE', pageParam),
    initialPageParam: 1,
    getNextPageParam: (last, pages) => (last.hasMore ? pages.length + 1 : undefined),
    enabled: showMoreDone,
  })
  const extraDone = useMemo(() => moreDone.data?.pages.flatMap((page) => page.items) ?? [], [moreDone.data])

  const setTask = (taskId: string | null) =>
    setSearchParams(
      (params) => {
        if (taskId) params.set('task', taskId)
        else params.delete('task')
        return params
      },
      { replace: !taskId },
    )

  if (board.isPending) {
    return (
      <div className="grid gap-3 lg:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => (
          <Skeleton key={index} className="h-80 rounded-xl" />
        ))}
      </div>
    )
  }
  if (board.isError) return <ErrorState error={board.error} onRetry={() => board.refetch()} />

  const data = board.data
  const doneTotal = data.columns.find((column) => column.status === 'DONE')?.totalCount ?? 0
  const doneLoaded = Math.min(doneTotal, DONE_PAGE_SIZE) + extraDone.length
  const doneHasMore = showMoreDone ? (moreDone.hasNextPage ?? false) : doneTotal > DONE_PAGE_SIZE

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <div className="flex -space-x-2">
            {data.members.slice(0, 6).map((member) => (
              <UserAvatar key={member.userId} name={member.name} id={member.userId} className="ring-2 ring-background" />
            ))}
          </div>
          <span className="text-sm text-muted-foreground">
            {data.members.length} {data.members.length === 1 ? 'member' : 'members'}
          </span>
        </div>
        {data.permissions.canCreateTasks ? (
          <Button onClick={() => setCreateIn('BACKLOG')}>
            <Plus /> New task
          </Button>
        ) : null}
      </div>

      <KanbanBoard
        board={data}
        extraDone={extraDone}
        onOpenTask={(taskId) => setTask(taskId)}
        onAddTask={setCreateIn}
        doneFooter={
          doneHasMore ? (
            <Button
              variant="ghost"
              size="sm"
              className="mx-2 mb-2"
              disabled={moreDone.isFetching}
              onClick={() => (showMoreDone ? moreDone.fetchNextPage() : setShowMoreDone(true))}
            >
              {moreDone.isFetching ? <Loader2 className="animate-spin" /> : null}
              Show more ({doneTotal - doneLoaded} older)
            </Button>
          ) : null
        }
      />

      <TaskFormDialog
        projectId={project.id}
        status={createIn}
        onClose={() => setCreateIn(null)}
        members={data.members}
        tags={data.tags}
      />
      <TaskSheet
        projectId={project.id}
        taskId={openTaskId}
        members={data.members}
        tags={data.tags}
        canCreateTags={data.permissions.canManage}
        onClose={() => setTask(null)}
      />
    </div>
  )
}
