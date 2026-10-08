import type { Board, TaskCard, TaskStatus } from '@/api/types'
import { STATUSES } from '@/lib/labels'

export type Columns = Record<TaskStatus, TaskCard[]>

export const COLUMN_PREFIX = 'column:'

export function columnId(status: TaskStatus) {
  return `${COLUMN_PREFIX}${status}`
}

export function columnsFromBoard(board: Board, extraDone: TaskCard[] = []): Columns {
  const columns = Object.fromEntries(STATUSES.map((status) => [status, [] as TaskCard[]])) as Columns
  for (const column of board.columns) columns[column.status] = [...column.tasks]
  if (extraDone.length > 0) {
    const known = new Set(columns.DONE.map((task) => task.id))
    columns.DONE = [...columns.DONE, ...extraDone.filter((task) => !known.has(task.id))]
  }
  return columns
}

/** Column holding a card, or the column itself when the id is a droppable column id. */
export function findColumn(columns: Columns, id: string): TaskStatus | null {
  if (id.startsWith(COLUMN_PREFIX)) return id.slice(COLUMN_PREFIX.length) as TaskStatus
  for (const status of STATUSES) {
    if (columns[status].some((task) => task.id === id)) return status
  }
  return null
}

/**
 * The cards directly above and below {@code taskId} in its final column; this is what the server needs to
 * place it (positions are computed server-side).
 */
export function neighboursOf(column: TaskCard[], taskId: string) {
  const index = column.findIndex((task) => task.id === taskId)
  return {
    previousTaskId: index > 0 ? column[index - 1].id : null,
    nextTaskId: index >= 0 && index < column.length - 1 ? column[index + 1].id : null,
  }
}

/** Optimistic version of a move, applied to the cached board. */
export function applyMove(board: Board, taskId: string, status: TaskStatus, index: number): Board {
  let moved: TaskCard | undefined
  const withoutCard = board.columns.map((column) => {
    const found = column.tasks.find((task) => task.id === taskId)
    if (!found) return column
    moved = found
    return { ...column, tasks: column.tasks.filter((task) => task.id !== taskId), totalCount: column.totalCount - 1 }
  })
  if (!moved) return board
  const updated: TaskCard = {
    ...moved,
    status,
    dueState: status === 'DONE' ? 'COMPLETED' : moved.dueState === 'COMPLETED' ? 'NONE' : moved.dueState,
  }
  return {
    ...board,
    columns: withoutCard.map((column) => {
      if (column.status !== status) return column
      const tasks = [...column.tasks]
      tasks.splice(Math.max(0, Math.min(index, tasks.length)), 0, updated)
      return { ...column, tasks, totalCount: column.totalCount + 1 }
    }),
  }
}
