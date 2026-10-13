import { useId, useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { managementApi } from '@/api/endpoints'
import type { UserRow } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { FormError, FormField } from '@/components/common/FormField'
import { emailField, newPasswordField } from '@/features/auth/schemas'
import { applyFieldErrors, errorMessage } from '@/lib/errors'
import { ROLES, ROLE_HINT, ROLE_LABEL } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'

// Mirrors the backend limits; the starting password follows the same rules as a self-chosen one.
const baseSchema = z.object({
  name: z.string().trim().min(1, 'Full name is required.').max(100, 'Full name must be at most 100 characters.'),
  email: emailField,
  jobTitle: z.string().trim().max(100, 'Job title must be at most 100 characters.'),
  role: z.enum(['IT_MANAGER', 'PROJECT_OWNER', 'MEMBER']),
  password: z.string(),
  confirmPassword: z.string(),
  currentPassword: z.string().min(1, 'Enter your password to confirm.'),
})

type Values = z.infer<typeof baseSchema>

/** Role and starting password only exist when creating; editing changes the details. */
function schemaFor(creating: boolean) {
  return baseSchema.superRefine((values, ctx) => {
    if (!creating) return
    const password = newPasswordField.safeParse(values.password)
    if (!password.success) {
      ctx.addIssue({ code: 'custom', path: ['password'], message: password.error.issues[0]?.message ?? 'Invalid password.' })
    } else if (!values.confirmPassword) {
      ctx.addIssue({ code: 'custom', path: ['confirmPassword'], message: 'Please confirm the password.' })
    } else if (values.confirmPassword !== values.password) {
      ctx.addIssue({ code: 'custom', path: ['confirmPassword'], message: 'Passwords do not match.' })
    }
  })
}

const SERVER_FIELDS = ['name', 'email', 'jobTitle', 'role', 'password', 'confirmPassword', 'currentPassword']

interface UserFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Edit mode when provided. */
  user?: UserRow
  onSaved?: (user: UserRow) => void
}

/** The IT Manager creates an account (with a starting password to share) or corrects one's details. */
export function UserFormDialog({ open, onOpenChange, user, onSaved }: UserFormDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100svh-2rem)] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{user ? `Edit ${user.name}` : 'Add a user'}</DialogTitle>
          <DialogDescription>
            {user
              ? 'Correct their name, sign-in email or job title.'
              : 'Create an account and choose a starting password to share with them. They can change it from their profile.'}
          </DialogDescription>
        </DialogHeader>
        {/* Mounted only while open, so passwords never linger between openings. */}
        <UserForm user={user} onClose={() => onOpenChange(false)} onSaved={onSaved} />
      </DialogContent>
    </Dialog>
  )
}

function UserForm({ user, onClose, onSaved }: { user?: UserRow; onClose: () => void; onSaved?: (user: UserRow) => void }) {
  const creating = !user
  // Unique ids: this dialog opens over pages that have their own fields (the Users page's Role filter).
  const baseId = useId()
  const id = (field: string) => `${baseId}-${field}`
  const queryClient = useQueryClient()
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schemaFor(creating)),
    defaultValues: {
      name: user?.name ?? '',
      email: user?.email ?? '',
      jobTitle: user?.jobTitle ?? '',
      role: user?.role ?? 'MEMBER',
      password: '',
      confirmPassword: '',
      currentPassword: '',
    },
  })
  const role = useWatch({ control, name: 'role' })

  const save = useMutation({
    mutationFn: (values: Values) => {
      const details = { name: values.name, email: values.email, jobTitle: values.jobTitle.trim() || null }
      return user
        ? managementApi.updateUser(user.id, { ...details, currentPassword: values.currentPassword })
        : managementApi.createUser({
            ...details,
            role: values.role,
            password: values.password,
            confirmPassword: values.confirmPassword,
            currentPassword: values.currentPassword,
          })
    },
    onSuccess: (saved) => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.management.all })
      if (user) {
        toast.success(saved.email !== user.email ? `Saved. ${saved.name} now signs in with ${saved.email}.` : 'Account details saved.')
      } else {
        toast.success(`Account created for ${saved.name}. Share the starting password with them.`)
      }
      onSaved?.(saved)
      onClose()
    },
    onError: (error) => {
      if (!applyFieldErrors(error, setError, SERVER_FIELDS)) setFormError(errorMessage(error))
    },
  })

  const submit = handleSubmit((values) => {
    setFormError(null)
    save.mutate(values)
  })

  // The IT Manager's own password lives in a separate <form>: in one form with someone else's new password,
  // browsers take it for a change of the IT Manager's password and offer to overwrite their saved one.
  return (
    <div className="grid gap-4">
      <form className="grid gap-4" onSubmit={submit} noValidate>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Full name" htmlFor={id('name')} error={errors.name?.message}>
            <Input id={id('name')} autoComplete="off" aria-invalid={!!errors.name} autoFocus {...register('name')} />
          </FormField>
          <FormField
            label="Email"
            htmlFor={id('email')}
            error={errors.email?.message}
            hint={creating ? undefined : 'They sign in with this email.'}
          >
            <Input id={id('email')} type="email" autoComplete="off" aria-invalid={!!errors.email} {...register('email')} />
          </FormField>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Job title" htmlFor={id('job-title')} error={errors.jobTitle?.message} hint="Optional">
            <Input id={id('job-title')} autoComplete="off" aria-invalid={!!errors.jobTitle} {...register('jobTitle')} />
          </FormField>
          {creating ? (
            <FormField label="Role" htmlFor={id('role')} error={errors.role?.message} hint={ROLE_HINT[role]}>
              <Controller
                control={control}
                name="role"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id={id('role')} className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {ROLES.map((value) => (
                        <SelectItem key={value} value={value}>
                          {ROLE_LABEL[value]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </FormField>
          ) : null}
        </div>
        {creating ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label="Starting password" htmlFor={id('password')} error={errors.password?.message} hint="At least 8 characters.">
              <Input
                id={id('password')}
                type="password"
                autoComplete="new-password"
                aria-invalid={!!errors.password}
                {...register('password')}
              />
            </FormField>
            <FormField label="Confirm password" htmlFor={id('confirm-password')} error={errors.confirmPassword?.message}>
              <Input
                id={id('confirm-password')}
                type="password"
                autoComplete="new-password"
                aria-invalid={!!errors.confirmPassword}
                {...register('confirmPassword')}
              />
            </FormField>
          </div>
        ) : null}
      </form>
      <form className="grid gap-4 border-t pt-4" onSubmit={submit} noValidate>
        <FormField
          label="Your password"
          htmlFor={id('admin-password')}
          error={errors.currentPassword?.message}
          hint="Confirms that you are making this change."
        >
          <Input
            id={id('admin-password')}
            type="password"
            autoComplete="current-password"
            aria-invalid={!!errors.currentPassword}
            {...register('currentPassword')}
          />
        </FormField>
        {/* Next to the buttons, so it stays in view when the dialog scrolls on a phone. */}
        <FormError message={formError} />
        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose} disabled={save.isPending}>
            Cancel
          </Button>
          <Button type="submit" disabled={save.isPending}>
            {save.isPending ? <Loader2 className="animate-spin" /> : null}
            {creating ? 'Create account' : 'Save changes'}
          </Button>
        </DialogFooter>
      </form>
    </div>
  )
}
