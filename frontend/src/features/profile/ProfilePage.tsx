import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { authApi } from '@/api/endpoints'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { FormError, FormField } from '@/components/common/FormField'
import { PageHeader } from '@/components/common/PageHeader'
import { UserAvatar } from '@/components/common/UserAvatar'
import { changePasswordSchema, type ChangePasswordValues } from '@/features/auth/schemas'
import { useAuth, useCurrentUser } from '@/features/auth/use-auth'
import { applyFieldErrors, errorMessage } from '@/lib/errors'
import { formatLongDay } from '@/lib/format'
import { ROLE_LABEL } from '@/lib/labels'

const profileSchema = z.object({
  name: z.string().trim().min(1, 'Full name is required.').max(100, 'Full name must be at most 100 characters.'),
  jobTitle: z.string().trim().max(100, 'Job title must be at most 100 characters.'),
})

/** User profile (claude.md §4): name, job title and password. Role and email are managed by IT. */
export function ProfilePage() {
  const user = useCurrentUser()
  return (
    <div className="mx-auto grid max-w-2xl gap-6">
      <PageHeader title="Profile" description="How you appear to your teammates." className="mb-0" />
      <section className="flex items-center gap-4 rounded-xl border bg-card p-5 shadow-xs">
        <UserAvatar name={user.name} id={user.id} size="lg" />
        <div className="min-w-0">
          <p className="truncate font-semibold">{user.name}</p>
          <p className="truncate text-sm text-muted-foreground">{user.email}</p>
          <p className="mt-1 text-xs text-muted-foreground">
            {ROLE_LABEL[user.role]} · Member since {formatLongDay(user.createdAt)}
          </p>
        </div>
      </section>
      <ProfileForm />
      <PasswordForm />
    </div>
  )
}

function ProfileForm() {
  const user = useCurrentUser()
  const { updateUser } = useAuth()
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<z.infer<typeof profileSchema>>({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: user.name, jobTitle: user.jobTitle ?? '' },
  })

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      const profile = await authApi.updateProfile({ name: values.name, jobTitle: values.jobTitle || null })
      updateUser(profile)
      reset({ name: profile.name, jobTitle: profile.jobTitle ?? '' })
      toast.success('Profile updated.')
    } catch (error) {
      if (!applyFieldErrors(error, setError, ['name', 'jobTitle'])) setFormError(errorMessage(error))
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="grid gap-4 rounded-xl border bg-card p-5 shadow-xs">
      <h2 className="font-semibold">Personal information</h2>
      <FormError message={formError} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Full name" htmlFor="profile-name" error={errors.name?.message}>
          <Input id="profile-name" autoComplete="name" aria-invalid={!!errors.name} {...register('name')} />
        </FormField>
        <FormField label="Job title" htmlFor="profile-job" error={errors.jobTitle?.message} hint="Shown in project member lists.">
          <Input id="profile-job" placeholder="Backend Developer" aria-invalid={!!errors.jobTitle} {...register('jobTitle')} />
        </FormField>
      </div>
      <div>
        <Button type="submit" disabled={isSubmitting || !isDirty}>
          {isSubmitting ? <Loader2 className="animate-spin" /> : null}
          Save profile
        </Button>
      </div>
    </form>
  )
}

function PasswordForm() {
  const { startSession } = useAuth()
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      startSession(await authApi.changePassword(values))
      reset()
      toast.success('Password changed. Your other devices were signed out.')
    } catch (error) {
      if (!applyFieldErrors(error, setError, ['currentPassword', 'newPassword', 'confirmPassword'])) {
        setFormError(errorMessage(error))
      }
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="grid gap-4 rounded-xl border bg-card p-5 shadow-xs">
      <div>
        <h2 className="font-semibold">Password</h2>
        <p className="text-sm text-muted-foreground">Changing it signs you out everywhere else.</p>
      </div>
      <FormError message={formError} />
      <FormField label="Current password" htmlFor="current-password" error={errors.currentPassword?.message}>
        <Input id="current-password" type="password" autoComplete="current-password" aria-invalid={!!errors.currentPassword} {...register('currentPassword')} />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="New password" htmlFor="new-password" error={errors.newPassword?.message}>
          <Input id="new-password" type="password" autoComplete="new-password" aria-invalid={!!errors.newPassword} {...register('newPassword')} />
        </FormField>
        <FormField label="Confirm new password" htmlFor="confirm-new-password" error={errors.confirmPassword?.message}>
          <Input
            id="confirm-new-password"
            type="password"
            autoComplete="new-password"
            aria-invalid={!!errors.confirmPassword}
            {...register('confirmPassword')}
          />
        </FormField>
      </div>
      <div>
        <Button type="submit" variant="outline" disabled={isSubmitting}>
          {isSubmitting ? <Loader2 className="animate-spin" /> : null}
          Change password
        </Button>
      </div>
    </form>
  )
}
