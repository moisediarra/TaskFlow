import { describe, expect, it } from 'vitest'
import type { Board, TaskCard, TaskStatus } from '@/api/types'
import { applyMove, columnId, columnsFromBoard, findColumn, neighboursOf } from './board-utils'

function card(id: string, status: TaskStatus, dueState: TaskCard['dueState'] = 'NONE'): TaskCard {
  return {
    id,
    title: id,
    status,
    priority: 'MEDIUM',
    dueDate: null,
    dueState,
    position: 0,
    assignee: null,
    tags: [],
    permissions: { canEdit: true, canMove: true, canDelete: true, canAssign: true },
    updatedAt: '2026-10-07T10:00:00Z',
  }
}

function board(): Board {
  const columns = {
    BACKLOG: [card('a', 'BACKLOG')],
    TODO: [card('b', 'TODO'), card('c', 'TODO')],
    IN_PROGRESS: [],
    DONE: [card('d', 'DONE', 'COMPLETED')],
  }
  return {
    project: { id: 'p', name: 'P', description: null, ownerId: 'o', ownerName: 'Owner' },
    permissions: { canManage: true, canCreateTasks: true },
    members: [],
    tags: [],
    columns: (Object.keys(columns) as TaskStatus[]).map((status) => ({
      status,
      label: status,
      tasks: columns[status],
      totalCount: columns[status].length,
    })),
  }
}

describe('board utils', () => {
  it('finds the column of a card or of a column id', () => {
    const columns = columnsFromBoard(board())
    expect(findColumn(columns, 'c')).toBe('TODO')
    expect(findColumn(columns, columnId('IN_PROGRESS'))).toBe('IN_PROGRESS')
    expect(findColumn(columns, 'missing')).toBeNull()
  })

  it('reports the neighbours the server needs to place a card', () => {
    const column = [card('x', 'TODO'), card('y', 'TODO'), card('z', 'TODO')]
    expect(neighboursOf(column, 'y')).toEqual({ previousTaskId: 'x', nextTaskId: 'z' })
    expect(neighboursOf(column, 'x')).toEqual({ previousTaskId: null, nextTaskId: 'y' })
    expect(neighboursOf(column, 'z')).toEqual({ previousTaskId: 'y', nextTaskId: null })
  })

  it('moves a card optimistically and keeps column totals in sync', () => {
    const moved = applyMove(board(), 'b', 'IN_PROGRESS', 0)
    const todo = moved.columns.find((column) => column.status === 'TODO')!
    const inProgress = moved.columns.find((column) => column.status === 'IN_PROGRESS')!
    expect(todo.tasks.map((task) => task.id)).toEqual(['c'])
    expect(todo.totalCount).toBe(1)
    expect(inProgress.tasks.map((task) => task.id)).toEqual(['b'])
    expect(inProgress.tasks[0].status).toBe('IN_PROGRESS')
  })

  it('never shows a completed task as overdue', () => {
    const overdue = board()
    overdue.columns[1].tasks[0] = { ...overdue.columns[1].tasks[0], dueState: 'OVERDUE' }
    const done = applyMove(overdue, 'b', 'DONE', 0)
    expect(done.columns[3].tasks[0].dueState).toBe('COMPLETED')
  })

  it('appends extra Done pages without duplicates', () => {
    const columns = columnsFromBoard(board(), [card('d', 'DONE'), card('e', 'DONE')])
    expect(columns.DONE.map((task) => task.id)).toEqual(['d', 'e'])
  })
})
