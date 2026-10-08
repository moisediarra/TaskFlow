import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { authApi } from '@/api/endpoints'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { FormError, FormField } from '@/components/common/FormField'
import { applyFieldErrors, errorMessage } from '@/lib/errors'
import { resetPasswordSchema, type ResetPasswordValues } from './schemas'

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { password: '', confirmPassword: '' },
  })

  if (!token) {
    return (
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Invalid reset link</h1>
        <p className="mt-2 text-sm text-muted-foreground">This link is incomplete. Request a new one to continue.</p>
        <Button asChild className="mt-6">
          <Link to="/forgot-password">Request a new link</Link>
        </Button>
      </div>
    )
  }

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await authApi.resetPassword({ token, ...values })
      toast.success('Your password was changed. Sign in with your new password.')
      navigate('/login', { replace: true })
    } catch (error) {
      if (!applyFieldErrors(error, setError, ['password', 'confirmPassword'])) setFormError(errorMessage(error))
    }
  })

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">Choose a new password</h1>
      <p className="mt-1 text-sm text-muted-foreground">You'll be signed out on your other devices.</p>
      <form className="mt-6 grid gap-4" onSubmit={onSubmit} noValidate>
        <FormError message={formError} />
        <FormField label="New password" htmlFor="password" error={errors.password?.message} hint="At least 8 characters.">
          <Input id="password" type="password" autoComplete="new-password" autoFocus aria-invalid={!!errors.password} {...register('password')} />
        </FormField>
        <FormField label="Confirm new password" htmlFor="confirmPassword" error={errors.confirmPassword?.message}>
          <Input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            aria-invalid={!!errors.confirmPassword}
            {...register('confirmPassword')}
          />
        </FormField>
        <Button type="submit" size="lg" disabled={isSubmitting}>
          {isSubmitting ? <Loader2 className="animate-spin" /> : null}
          Change password
        </Button>
      </form>
      {formError ? (
        <p className="mt-4 text-center text-sm">
          <Link to="/forgot-password" className="text-primary hover:underline">
            Request a new reset link
          </Link>
        </p>
      ) : null}
    </div>
  )
}
