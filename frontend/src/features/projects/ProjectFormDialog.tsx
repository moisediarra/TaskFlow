import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { projectsApi } from '@/api/endpoints'
import type { ProjectDetail } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { FormError, FormField } from '@/components/common/FormField'
import { applyFieldErrors, errorMessage } from '@/lib/errors'
import { queryKeys } from '@/lib/query-keys'
import { useState } from 'react'

const schema = z.object({
  name: z.string().trim().min(1, 'Project name is required.').max(100, 'Project name must be at most 100 characters.'),
  description: z.string().max(2000, 'Description must be at most 2000 characters.'),
})

type Values = z.infer<typeof schema>

interface ProjectFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Edit mode when provided. */
  project?: Pick<ProjectDetail, 'id' | 'name' | 'description'>
  onSaved?: (project: ProjectDetail) => void
}

export function ProjectFormDialog({ open, onOpenChange, project, onSaved }: ProjectFormDialogProps) {
  const queryClient = useQueryClient()
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { name: '', description: '' } })

  useEffect(() => {
    if (open) {
      reset({ name: project?.name ?? '', description: project?.description ?? '' })
      setFormError(null)
    }
  }, [open, project, reset])

  const save = useMutation({
    mutationFn: (values: Values) => {
      const body = { name: values.name, description: values.description.trim() || null }
      return project ? projectsApi.update(project.id, body) : projectsApi.create(body)
    },
    onSuccess: (saved) => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.projects })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard })
      toast.success(project ? 'Project updated.' : `Project “${saved.name}” created.`)
      onOpenChange(false)
      onSaved?.(saved)
    },
    onError: (error) => {
      if (!applyFieldErrors(error, setError, ['name', 'description'])) setFormError(errorMessage(error))
    },
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{project ? 'Edit project' : 'Create a project'}</DialogTitle>
          <DialogDescription>
            {project ? 'Update the name and description.' : 'Give your project a name; you can add members and tasks next.'}
          </DialogDescription>
        </DialogHeader>
        <form className="grid gap-4" onSubmit={handleSubmit((values) => save.mutate(values))} noValidate>
          <FormError message={formError} />
          <FormField label="Name" htmlFor="project-name" error={errors.name?.message}>
            <Input id="project-name" placeholder="Mobile Banking App" autoFocus aria-invalid={!!errors.name} {...register('name')} />
          </FormField>
          <FormField label="Description" htmlFor="project-description" error={errors.description?.message}>
            <Textarea
              id="project-description"
              rows={4}
              placeholder="What is this project about?"
              aria-invalid={!!errors.description}
              {...register('description')}
            />
          </FormField>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={save.isPending}>
              {save.isPending ? <Loader2 className="animate-spin" /> : null}
              {project ? 'Save changes' : 'Create project'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
