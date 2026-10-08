import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { tasksApi } from '@/api/endpoints'
import type { Board, NewTaskInput, TaskInput, TaskStatus } from '@/api/types'
import { applyMove } from '@/features/board/board-utils'
import { errorMessage } from '@/lib/errors'
import { queryKeys } from '@/lib/query-keys'

/** Everything derived from a project's tasks: board, overview, activity, dashboard, project cards. */
export function invalidateProjectWork(queryClient: QueryClient, projectId: string, taskId?: string) {
  void queryClient.invalidateQueries({ queryKey: queryKeys.project(projectId) })
  void queryClient.invalidateQueries({ queryKey: queryKeys.projects, exact: true })
  void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard })
  void queryClient.invalidateQueries({ queryKey: queryKeys.management.all })
  if (taskId) void queryClient.invalidateQueries({ queryKey: queryKeys.task(taskId) })
}

interface MoveVariables {
  taskId: string
  status: TaskStatus
  index: number
  previousTaskId: string | null
  nextTaskId: string | null
}

/** Drag and drop / status change: the board updates immediately and rolls back if the server refuses. */
export function useMoveTask(projectId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ taskId, status, previousTaskId, nextTaskId }: MoveVariables) =>
      tasksApi.move(taskId, { status, previousTaskId, nextTaskId }),
    onMutate: async ({ taskId, status, index }) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.board(projectId) })
      const previous = queryClient.getQueryData<Board>(queryKeys.board(projectId))
      if (previous) queryClient.setQueryData(queryKeys.board(projectId), applyMove(previous, taskId, status, index))
      return { previous }
    },
    onError: (error, _variables, context) => {
      if (context?.previous) queryClient.setQueryData(queryKeys.board(projectId), context.previous)
      toast.error(errorMessage(error, "The task couldn't be moved. Please try again."))
    },
    onSettled: (_data, _error, { taskId }) => invalidateProjectWork(queryClient, projectId, taskId),
  })
}

export function useCreateTask(projectId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: NewTaskInput) => tasksApi.create(projectId, input),
    onSuccess: (task) => {
      toast.success(`Task “${task.title}” created.`)
      invalidateProjectWork(queryClient, projectId)
    },
  })
}

export function useUpdateTask(projectId: string, taskId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: TaskInput) => tasksApi.update(taskId, input),
    onSuccess: (task) => {
      queryClient.setQueryData(queryKeys.task(taskId), task)
      toast.success('Task updated.')
      invalidateProjectWork(queryClient, projectId, taskId)
    },
  })
}

export function useAssignTask(projectId: string, taskId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (assigneeId: string | null) => tasksApi.assign(taskId, assigneeId),
    onSuccess: (task) => {
      queryClient.setQueryData(queryKeys.task(taskId), task)
      toast.success(task.assignee ? `Assigned to ${task.assignee.name}.` : 'Task unassigned.')
      invalidateProjectWork(queryClient, projectId, taskId)
    },
    onError: (error) => toast.error(errorMessage(error, "The assignee couldn't be changed.")),
  })
}

export function useDeleteTask(projectId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (taskId: string) => tasksApi.remove(taskId),
    onSuccess: (_data, taskId) => {
      queryClient.removeQueries({ queryKey: queryKeys.task(taskId) })
      toast.success('Task deleted.')
      invalidateProjectWork(queryClient, projectId)
    },
    onError: (error) => toast.error(errorMessage(error, "The task couldn't be deleted.")),
  })
}
