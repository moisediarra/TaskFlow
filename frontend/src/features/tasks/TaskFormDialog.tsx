import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import type { ProjectMember, Tag, TaskStatus } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { FormError } from '@/components/common/FormField'
import { applyFieldErrors, errorMessage } from '@/lib/errors'
import { STATUS_LABEL } from '@/lib/labels'
import { useCreateTask } from './hooks'
import { TaskFields } from './TaskFields'
import { TASK_FORM_FIELDS, UNASSIGNED, taskFormSchema, toTaskInput, type TaskFormValues } from './task-form'

interface TaskFormDialogProps {
  projectId: string
  /** The column the task is created from decides its status (claude.md §13); null closes the dialog. */
  status: TaskStatus | null
  onClose: () => void
  members: ProjectMember[]
  tags: Tag[]
}

const EMPTY: TaskFormValues = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueDate: '',
  assigneeId: UNASSIGNED,
  tagIds: [],
  newTags: [],
}

export function TaskFormDialog({ projectId, status, onClose, members, tags }: TaskFormDialogProps) {
  const create = useCreateTask(projectId)
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<TaskFormValues>({ resolver: zodResolver(taskFormSchema), defaultValues: EMPTY })

  useEffect(() => {
    if (status) {
      reset(EMPTY)
      setFormError(null)
    }
  }, [status, reset])

  const onSubmit = handleSubmit((values) => {
    if (!status) return
    setFormError(null)
    create.mutate(
      { ...toTaskInput(values), status, assigneeId: values.assigneeId === UNASSIGNED ? null : values.assigneeId },
      {
        onSuccess: onClose,
        onError: (error) => {
          if (!applyFieldErrors(error, setError, TASK_FORM_FIELDS)) setFormError(errorMessage(error))
        },
      },
    )
  })

  return (
    <Dialog open={status !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[92svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>New task</DialogTitle>
          <DialogDescription>{status ? `It will be added to ${STATUS_LABEL[status]}.` : null}</DialogDescription>
        </DialogHeader>
        <form className="grid gap-4" onSubmit={onSubmit} noValidate>
          <FormError message={formError} />
          <TaskFields
            idPrefix="new-task"
            register={register}
            control={control}
            errors={errors}
            tags={tags}
            canCreateTags
            members={members}
          />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={create.isPending}>
              {create.isPending ? <Loader2 className="animate-spin" /> : null}
              Create task
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
