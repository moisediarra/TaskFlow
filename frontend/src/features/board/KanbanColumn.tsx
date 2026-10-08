import type { ReactNode } from 'react'
import { useDroppable } from '@dnd-kit/core'
import { SortableContext, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Plus } from 'lucide-react'
import type { TaskCard, TaskStatus } from '@/api/types'
import { Button } from '@/components/ui/button'
import { STATUS_DOT, STATUS_LABEL } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { columnId } from './board-utils'
import { TaskCardView } from './TaskCardView'

interface KanbanColumnProps {
  status: TaskStatus
  tasks: TaskCard[]
  count: number
  canAdd: boolean
  onAdd: () => void
  onOpenTask: (taskId: string) => void
  footer?: ReactNode
}

/** One board column; empty columns are still drop targets (claude.md §36 empty state). */
export function KanbanColumn({ status, tasks, count, canAdd, onAdd, onOpenTask, footer }: KanbanColumnProps) {
  const { setNodeRef, isOver } = useDroppable({ id: columnId(status) })
  const label = STATUS_LABEL[status]
  return (
    <section
      aria-label={`${label} column`}
      className="flex w-[82vw] max-w-80 shrink-0 snap-start flex-col rounded-xl bg-secondary/80 sm:w-72 lg:w-auto lg:max-w-none lg:min-w-0"
    >
      <header className="flex items-center justify-between gap-2 px-3 pt-3 pb-2">
        <h2 className="flex items-center gap-2 text-sm font-semibold">
          <span className={cn('size-2 rounded-full', STATUS_DOT[status])} aria-hidden />
          {label}
          <span className="rounded-full bg-background px-1.5 text-xs font-medium text-muted-foreground tabular-nums">{count}</span>
        </h2>
        {canAdd ? (
          <Button variant="ghost" size="icon-sm" onClick={onAdd} aria-label={`Add a task to ${label}`}>
            <Plus />
          </Button>
        ) : null}
      </header>
      <SortableContext items={tasks.map((task) => task.id)} strategy={verticalListSortingStrategy}>
        <div
          ref={setNodeRef}
          className={cn(
            'mx-2 mb-2 flex min-h-28 flex-1 flex-col gap-2 rounded-lg transition-colors',
            isOver && 'bg-accent/70 outline-2 outline-dashed outline-primary/30',
          )}
        >
          {tasks.map((task) => (
            <SortableCard key={task.id} task={task} onOpen={onOpenTask} />
          ))}
          {tasks.length === 0 ? (
            <div className="flex flex-1 flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-border/80 px-3 py-6 text-center">
              <p className="text-sm text-muted-foreground">No tasks here.</p>
              {canAdd ? (
                <Button variant="link" size="sm" onClick={onAdd}>
                  <Plus /> Add Task
                </Button>
              ) : null}
            </div>
          ) : null}
        </div>
      </SortableContext>
      {canAdd && tasks.length > 0 ? (
        <button
          type="button"
          onClick={onAdd}
          className="mx-2 mb-2 flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-background hover:text-foreground"
        >
          <Plus className="size-4" /> Add Task
        </button>
      ) : null}
      {footer}
    </section>
  )
}

function SortableCard({ task, onOpen }: { task: TaskCard; onOpen: (taskId: string) => void }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
    disabled: !task.permissions.canMove,
  })
  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      {...attributes}
      {...listeners}
      role="button"
      tabIndex={0}
      aria-label={`${task.title}. ${task.permissions.canMove ? 'Press Enter to open, Space to move.' : 'Press Enter to open.'}`}
      onClick={() => onOpen(task.id)}
      onKeyDown={(event) => {
        if (event.key === 'Enter') {
          event.preventDefault()
          onOpen(task.id)
        } else {
          listeners?.onKeyDown?.(event)
        }
      }}
      className={cn(
        'group/card touch-manipulation rounded-lg outline-none',
        task.permissions.canMove ? 'cursor-grab active:cursor-grabbing' : 'cursor-pointer',
      )}
    >
      <TaskCardView task={task} dragging={isDragging} />
    </div>
  )
}
