import { useMemo, useRef, useState, type ReactNode } from 'react'
import {
  DndContext,
  DragOverlay,
  KeyboardCode,
  KeyboardSensor,
  MouseSensor,
  TouchSensor,
  closestCorners,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
} from '@dnd-kit/core'
import { arrayMove, sortableKeyboardCoordinates } from '@dnd-kit/sortable'
import type { Board, TaskCard, TaskStatus } from '@/api/types'
import { useMoveTask } from '@/features/tasks/hooks'
import { STATUSES, STATUS_LABEL } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { COLUMN_PREFIX, columnsFromBoard, findColumn, neighboursOf, type Columns } from './board-utils'
import { KanbanColumn } from './KanbanColumn'
import { TaskCardView } from './TaskCardView'

interface KanbanBoardProps {
  board: Board
  extraDone: TaskCard[]
  onOpenTask: (taskId: string) => void
  onAddTask: (status: TaskStatus) => void
  doneFooter?: ReactNode
}

/**
 * Drag and drop between and within columns (claude.md §11). While dragging, a local copy of the columns
 * shows the preview; on drop the move is sent with the neighbouring cards and applied optimistically.
 */
export function KanbanBoard({ board, extraDone, onOpenTask, onAddTask, doneFooter }: KanbanBoardProps) {
  const move = useMoveTask(board.project.id)
  const base = useMemo(() => columnsFromBoard(board, extraDone), [board, extraDone])
  const [dragColumns, setDragColumns] = useState<Columns | null>(null)
  const [activeId, setActiveId] = useState<string | null>(null)
  const origin = useRef<{ status: TaskStatus; index: number } | null>(null)
  const columns = dragColumns ?? base

  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 5 } }),
    // A short press before dragging keeps horizontal scrolling of the board usable on phones.
    useSensor(TouchSensor, { activationConstraint: { delay: 200, tolerance: 8 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
      keyboardCodes: { start: [KeyboardCode.Space], cancel: [KeyboardCode.Esc], end: [KeyboardCode.Space] },
    }),
  )

  const activeTask = activeId ? STATUSES.flatMap((status) => columns[status]).find((task) => task.id === activeId) : undefined

  const onDragStart = ({ active }: DragStartEvent) => {
    const id = String(active.id)
    const status = findColumn(base, id)
    if (!status) return
    origin.current = { status, index: base[status].findIndex((task) => task.id === id) }
    setActiveId(id)
    setDragColumns(base)
  }

  const onDragOver = ({ active, over }: DragOverEvent) => {
    if (!over) return
    setDragColumns((current) => {
      if (!current) return current
      const id = String(active.id)
      const overId = String(over.id)
      const from = findColumn(current, id)
      const to = findColumn(current, overId)
      if (!from || !to || from === to) return current
      const card = current[from].find((task) => task.id === id)
      if (!card) return current
      const target = current[to]
      const overIndex = overId.startsWith(COLUMN_PREFIX) ? target.length : target.findIndex((task) => task.id === overId)
      const insertAt = overIndex < 0 ? target.length : overIndex
      return {
        ...current,
        [from]: current[from].filter((task) => task.id !== id),
        [to]: [...target.slice(0, insertAt), { ...card, status: to }, ...target.slice(insertAt)],
      }
    })
  }

  const finish = () => {
    setActiveId(null)
    setDragColumns(null)
    origin.current = null
  }

  const onDragEnd = ({ active, over }: DragEndEvent) => {
    const current = dragColumns
    const start = origin.current
    finish()
    if (!over || !current || !start) return
    const id = String(active.id)
    const overId = String(over.id)
    const status = findColumn(current, id)
    if (!status) return
    let column = current[status]
    if (!overId.startsWith(COLUMN_PREFIX) && overId !== id && findColumn(current, overId) === status) {
      column = arrayMove(
        column,
        column.findIndex((task) => task.id === id),
        column.findIndex((task) => task.id === overId),
      )
    }
    const index = column.findIndex((task) => task.id === id)
    if (status === start.status && index === start.index) return
    move.mutate({ taskId: id, status, index, ...neighboursOf(column, id) })
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={onDragStart}
      onDragOver={onDragOver}
      onDragEnd={onDragEnd}
      onDragCancel={finish}
      accessibility={{
        screenReaderInstructions: {
          draggable: 'To move a task, press Space, use the arrow keys to choose a place, then press Space again. Press Escape to cancel.',
        },
      }}
    >
      <div
        className={cn(
          '-mx-4 flex items-start gap-3 overflow-x-auto px-4 pb-4 sm:-mx-6 sm:px-6 lg:mx-0 lg:grid lg:grid-cols-4 lg:overflow-visible lg:px-0',
          activeId ? 'snap-none' : 'snap-x snap-mandatory lg:snap-none',
        )}
      >
        {STATUSES.map((status) => {
          const total = board.columns.find((column) => column.status === status)?.totalCount ?? 0
          return (
            <KanbanColumn
              key={status}
              status={status}
              tasks={columns[status]}
              count={status === 'DONE' ? Math.max(total, columns.DONE.length) : columns[status].length}
              canAdd={board.permissions.canCreateTasks && status !== 'DONE'}
              onAdd={() => onAddTask(status)}
              onOpenTask={onOpenTask}
              footer={status === 'DONE' ? doneFooter : undefined}
            />
          )
        })}
      </div>
      <DragOverlay dropAnimation={{ duration: 180, easing: 'ease-out' }}>
        {activeTask ? <TaskCardView task={activeTask} overlay /> : null}
      </DragOverlay>
      <p className="sr-only" aria-live="polite">
        {move.isPending && activeTask ? `Moving ${activeTask.title} to ${STATUS_LABEL[activeTask.status]}` : ''}
      </p>
    </DndContext>
  )
}
