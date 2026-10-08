import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { Loader2, Pencil, Trash2, UserPlus, X } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { projectsApi } from '@/api/endpoints'
import type { ProjectMember } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { FormField } from '@/components/common/FormField'
import { ErrorState, ListSkeleton } from '@/components/common/states'
import { UserAvatar } from '@/components/common/UserAvatar'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { applyFieldErrors, errorMessage } from '@/lib/errors'
import { queryKeys } from '@/lib/query-keys'
import { useProject } from './project-context'
import { ProjectFormDialog } from './ProjectFormDialog'

const addMemberSchema = z.object({
  email: z.string().trim().min(1, 'Email is required.').email('Enter a valid email address.'),
})

/** Owner-only settings: edit, members (claude.md §8) and deletion. */
export function ProjectSettingsPage() {
  const project = useProject()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [editing, setEditing] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [removing, setRemoving] = useState<ProjectMember | null>(null)
  const members = useQuery({ queryKey: queryKeys.members(project.id), queryFn: () => projectsApi.members(project.id) })

  const form = useForm<z.infer<typeof addMemberSchema>>({ resolver: zodResolver(addMemberSchema), defaultValues: { email: '' } })

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.project(project.id) })
    void queryClient.invalidateQueries({ queryKey: queryKeys.projects, exact: true })
  }

  const addMember = useMutation({
    mutationFn: (email: string) => projectsApi.addMember(project.id, email),
    onSuccess: (member) => {
      toast.success(`${member.name} was added to the project.`)
      form.reset({ email: '' })
      refresh()
    },
    onError: (error) => {
      if (!applyFieldErrors(error, form.setError, ['email'])) toast.error(errorMessage(error))
    },
  })

  const removeMember = useMutation({
    mutationFn: (member: ProjectMember) => projectsApi.removeMember(project.id, member.userId),
    onSuccess: (_data, member) => {
      toast.success(`${member.name} was removed. Their tasks in this project are now unassigned.`)
      setRemoving(null)
      refresh()
    },
    onError: (error) => toast.error(errorMessage(error)),
  })

  const deleteProject = useMutation({
    mutationFn: () => projectsApi.remove(project.id),
    onSuccess: () => {
      toast.success(`Project “${project.name}” was deleted.`)
      queryClient.removeQueries({ queryKey: queryKeys.project(project.id) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.projects })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard })
      navigate('/projects', { replace: true })
    },
    onError: (error) => toast.error(errorMessage(error)),
  })

  if (!project.permissions.canManage) return <ForbiddenPage />

  return (
    <div className="grid max-w-3xl gap-6">
      <section className="rounded-xl border bg-card p-5 shadow-xs">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h2 className="font-semibold">Project details</h2>
            <p className="mt-1 text-sm font-medium">{project.name}</p>
            <p className="mt-1 text-sm text-muted-foreground">{project.description ?? 'No description.'}</p>
          </div>
          <Button variant="outline" onClick={() => setEditing(true)}>
            <Pencil /> Edit
          </Button>
        </div>
      </section>

      <section className="rounded-xl border bg-card p-5 shadow-xs">
        <h2 className="font-semibold">Members</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Add teammates by the email they use for TaskFlow. Members can work on the tasks assigned to them.
        </p>
        <form
          className="mt-4 flex flex-col gap-2 sm:flex-row sm:items-start"
          onSubmit={form.handleSubmit(({ email }) => addMember.mutate(email))}
          noValidate
        >
          <FormField label="Email" htmlFor="member-email" error={form.formState.errors.email?.message} className="flex-1">
            <Input
              id="member-email"
              type="email"
              placeholder="sarah.smith@company.com"
              aria-invalid={!!form.formState.errors.email}
              {...form.register('email')}
            />
          </FormField>
          <Button type="submit" className="sm:mt-6" disabled={addMember.isPending}>
            {addMember.isPending ? <Loader2 className="animate-spin" /> : <UserPlus />}
            Add member
          </Button>
        </form>
        <div className="mt-5">
          {members.isPending ? (
            <ListSkeleton rows={3} />
          ) : members.isError ? (
            <ErrorState error={members.error} onRetry={() => members.refetch()} />
          ) : (
            <ul className="divide-y">
              {members.data.map((member) => (
                <li key={member.userId} className="flex items-center gap-3 py-2.5">
                  <UserAvatar name={member.name} id={member.userId} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">
                      {member.name}
                      {!member.active ? <span className="font-normal text-muted-foreground"> (deactivated)</span> : null}
                    </p>
                    <p className="truncate text-xs text-muted-foreground">
                      {member.jobTitle ? `${member.jobTitle} · ` : ''}
                      {member.email}
                    </p>
                  </div>
                  {member.role === 'OWNER' ? (
                    <span className="rounded-full bg-accent px-2 py-0.5 text-[11px] font-medium text-accent-foreground">Owner</span>
                  ) : (
                    <Button variant="ghost" size="sm" onClick={() => setRemoving(member)} aria-label={`Remove ${member.name}`}>
                      <X /> Remove
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="rounded-xl border border-destructive/30 bg-card p-5 shadow-xs">
        <h2 className="font-semibold text-destructive">Delete project</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Deletes the board, its tasks and tags for everyone. The activity history is kept for auditing.
        </p>
        <Button variant="destructive" className="mt-4" onClick={() => setConfirmDelete(true)}>
          <Trash2 /> Delete this project
        </Button>
      </section>

      <ProjectFormDialog open={editing} onOpenChange={setEditing} project={project} onSaved={refresh} />
      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(open) => !open && setRemoving(null)}
        title={`Remove ${removing?.name ?? 'member'}?`}
        description="They will lose access to this project and their tasks here will become unassigned."
        confirmLabel="Remove member"
        pending={removeMember.isPending}
        onConfirm={() => removing && removeMember.mutate(removing)}
      />
      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title={`Delete “${project.name}”?`}
        description="This permanently deletes the project, its tasks and tags. This can't be undone."
        confirmLabel="Delete project"
        pending={deleteProject.isPending}
        onConfirm={() => deleteProject.mutate()}
      />
    </div>
  )
}
