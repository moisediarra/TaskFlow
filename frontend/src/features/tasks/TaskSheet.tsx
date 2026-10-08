import { useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { Loader2, Lock, Pencil, Trash2 } from 'lucide-react'
import { tasksApi } from '@/api/endpoints'
import type { ProjectMember, Tag, TaskDetail, TaskStatus } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Separator } from '@/components/ui/separator'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { FormError } from '@/components/common/FormField'
import { ErrorState } from '@/components/common/states'
import { UserAvatar } from '@/components/common/UserAvatar'
import { applyFieldErrors, errorMessage } from '@/lib/errors'
import { formatLongDay } from '@/lib/format'
import { STATUSES, STATUS_LABEL } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'
import { DueBadge, PriorityBadge, StatusBadge, TagBadge } from './badges'
import { useAssignTask, useDeleteTask, useMoveTask, useUpdateTask } from './hooks'
import { TaskFields } from './TaskFields'
import { TASK_FORM_FIELDS, UNASSIGNED, taskFormSchema, toTaskInput, type TaskFormValues } from './task-form'

interface TaskSheetProps {
  projectId: string
  taskId: string | null
  members: ProjectMember[]
  tags: Tag[]
  canCreateTags: boolean
  onClose: () => void
}

/** Task details in a side panel (claude.md §14); actions follow the user's permissions. */
export function TaskSheet({ projectId, taskId, members, tags, canCreateTags, onClose }: TaskSheetProps) {
  const task = useQuery({
    queryKey: queryKeys.task(taskId ?? ''),
    queryFn: () => tasksApi.get(taskId!),
    enabled: !!taskId,
  })

  return (
    <Sheet open={!!taskId} onOpenChange={(open) => !open && onClose()}>
      <SheetContent side="right" className="w-full gap-0 overflow-y-auto data-[side=right]:sm:max-w-xl">
        {task.isPending ? (
          <div className="space-y-4 p-6">
            <SheetTitle className="sr-only">Loading task</SheetTitle>
            <Skeleton className="h-7 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-32 w-full" />
          </div>
        ) : task.isError ? (
          <div className="p-6">
            <SheetTitle className="sr-only">Task unavailable</SheetTitle>
            <ErrorState error={task.error} onRetry={() => task.refetch()} />
          </div>
        ) : (
          <TaskDetails
            key={task.data.id}
            task={task.data}
            projectId={projectId}
            members={members}
            tags={tags}
            canCreateTags={canCreateTags}
            onDeleted={onClose}
          />
        )}
      </SheetContent>
    </Sheet>
  )
}

function TaskDetails({
  task,
  projectId,
  members,
  tags,
  canCreateTags,
  onDeleted,
}: {
  task: TaskDetail
  projectId: string
  members: ProjectMember[]
  tags: Tag[]
  canCreateTags: boolean
  onDeleted: () => void
}) {
  const [editing, setEditing] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const move = useMoveTask(projectId)
  const assign = useAssignTask(projectId, task.id)
  const remove = useDeleteTask(projectId)
  const { permissions } = task

  const changeStatus = (status: TaskStatus) => {
    if (status === task.status) return
    move.mutate({ taskId: task.id, status, index: status === 'DONE' ? 0 : Number.MAX_SAFE_INTEGER, previousTaskId: null, nextTaskId: null })
  }

  if (editing) {
    return (
      <TaskEditForm
        task={task}
        projectId={projectId}
        tags={tags}
        canCreateTags={canCreateTags}
        onDone={() => setEditing(false)}
      />
    )
  }

  return (
    <>
      <SheetHeader className="border-b p-6 pr-12">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={task.status} />
          <PriorityBadge priority={task.priority} />
          <DueBadge dueDate={task.dueDate} dueState={task.dueState} />
        </div>
        <SheetTitle className="mt-2 text-xl leading-snug">{task.title}</SheetTitle>
        <SheetDescription>in {task.projectName}</SheetDescription>
      </SheetHeader>

      <div className="grid gap-6 p-6">
        <section>
          <h3 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Description</h3>
          {task.description ? (
            <p className="whitespace-pre-wrap text-sm leading-relaxed">{task.description}</p>
          ) : (
            <p className="text-sm text-muted-foreground">No description.</p>
          )}
        </section>

        <dl className="grid gap-4 text-sm sm:grid-cols-2">
          <Detail label="Status">
            {permissions.canMove ? (
              <Select value={task.status} onValueChange={(value) => changeStatus(value as TaskStatus)} disabled={move.isPending}>
                <SelectTrigger className="w-full" aria-label="Status">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {STATUSES.map((status) => (
                    <SelectItem key={status} value={status}>
                      {STATUS_LABEL[status]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            ) : (
              <StatusBadge status={task.status} />
            )}
          </Detail>
          <Detail label="Priority">
            <PriorityBadge priority={task.priority} />
          </Detail>
          <Detail label="Due date">{task.dueDate ? formatLongDay(task.dueDate) : <Muted>No due date</Muted>}</Detail>
          <Detail label="Assignee">
            {permissions.canAssign ? (
              <Select
                value={task.assignee?.id ?? UNASSIGNED}
                onValueChange={(value) => assign.mutate(value === UNASSIGNED ? null : value)}
                disabled={assign.isPending}
              >
                <SelectTrigger className="w-full" aria-label="Assignee">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={UNASSIGNED}>Unassigned</SelectItem>
                  {members
                    .filter((member) => member.active || member.userId === task.assignee?.id)
                    .map((member) => (
                      <SelectItem key={member.userId} value={member.userId}>
                        {member.name}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            ) : task.assignee ? (
              <span className="flex items-center gap-2">
                <UserAvatar name={task.assignee.name} id={task.assignee.id} size="sm" />
                {task.assignee.name}
                {!task.assignee.active ? <Muted>(deactivated)</Muted> : null}
              </span>
            ) : (
              <Muted>Unassigned</Muted>
            )}
          </Detail>
          <Detail label="Tags" wide>
            {task.tags.length > 0 ? (
              <div className="flex flex-wrap gap-1">
                {task.tags.map((tag) => (
                  <TagBadge key={tag.id} tag={tag} />
                ))}
              </div>
            ) : (
              <Muted>No tags</Muted>
            )}
          </Detail>
          <Detail label="Created">{formatLongDay(task.createdAt)}</Detail>
          <Detail label="Last updated">{formatLongDay(task.updatedAt)}</Detail>
        </dl>

        <Separator />

        {permissions.canEdit || permissions.canDelete ? (
          <div className="flex flex-wrap gap-2">
            {permissions.canEdit ? (
              <Button variant="outline" onClick={() => setEditing(true)}>
                <Pencil /> Edit task
              </Button>
            ) : null}
            {permissions.canDelete ? (
              <Button variant="destructive" onClick={() => setConfirmDelete(true)}>
                <Trash2 /> Delete
              </Button>
            ) : null}
          </div>
        ) : (
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <Lock className="size-4" /> You can view this task. Only its assignee or the project owner can change it.
          </p>
        )}
      </div>

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Delete this task?"
        description={<>“{task.title}” will be permanently deleted. Its history stays in the activity log.</>}
        confirmLabel="Delete task"
        pending={remove.isPending}
        onConfirm={() =>
          remove.mutate(task.id, {
            onSuccess: () => {
              setConfirmDelete(false)
              onDeleted()
            },
          })
        }
      />
    </>
  )
}

function TaskEditForm({
  task,
  projectId,
  tags,
  canCreateTags,
  onDone,
}: {
  task: TaskDetail
  projectId: string
  tags: Tag[]
  canCreateTags: boolean
  onDone: () => void
}) {
  const update = useUpdateTask(projectId, task.id)
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<TaskFormValues>({
    resolver: zodResolver(taskFormSchema),
    defaultValues: {
      title: task.title,
      description: task.description ?? '',
      priority: task.priority,
      dueDate: task.dueDate ?? '',
      assigneeId: task.assignee?.id ?? UNASSIGNED,
      tagIds: task.tags.map((tag) => tag.id),
      newTags: [],
    },
  })

  const onSubmit = handleSubmit((values) => {
    setFormError(null)
    update.mutate(toTaskInput(values), {
      onSuccess: onDone,
      onError: (error) => {
        if (!applyFieldErrors(error, setError, TASK_FORM_FIELDS)) setFormError(errorMessage(error))
      },
    })
  })

  return (
    <form onSubmit={onSubmit} noValidate className="flex min-h-full flex-col">
      <SheetHeader className="border-b p-6 pr-12">
        <SheetTitle>Edit task</SheetTitle>
        <SheetDescription>Changes are visible to the whole project.</SheetDescription>
      </SheetHeader>
      <div className="grid gap-4 p-6">
        <FormError message={formError} />
        <TaskFields
          idPrefix={`edit-${task.id}`}
          register={register}
          control={control}
          errors={errors}
          tags={tags}
          canCreateTags={canCreateTags}
        />
      </div>
      <div className="mt-auto flex justify-end gap-2 border-t p-4">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" disabled={update.isPending}>
          {update.isPending ? <Loader2 className="animate-spin" /> : null}
          Save changes
        </Button>
      </div>
    </form>
  )
}

function Detail({ label, children, wide }: { label: string; children: ReactNode; wide?: boolean }) {
  return (
    <div className={wide ? 'sm:col-span-2' : undefined}>
      <dt className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</dt>
      <dd>{children}</dd>
    </div>
  )
}

function Muted({ children }: { children: ReactNode }) {
  return <span className="text-muted-foreground">{children}</span>
}
