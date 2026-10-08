import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { FormError, FormField } from '@/components/common/FormField'
import { errorMessage } from '@/lib/errors'
import { homePath, safeNext } from './auth-context'
import { loginSchema, type LoginValues } from './schemas'
import { useAuth } from './use-auth'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const [formError, setFormError] = useState<string | null>(null)
  const prefilledEmail = (location.state as { email?: string } | null)?.email ?? ''
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: prefilledEmail, password: '' } })

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      const user = await login(values.email, values.password)
      navigate(safeNext(searchParams.get('next')) ?? homePath(user), { replace: true })
    } catch (error) {
      setFormError(errorMessage(error))
    }
  })

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">Welcome back</h1>
      <p className="mt-1 text-sm text-muted-foreground">Sign in to see your projects and tasks.</p>
      <form className="mt-6 grid gap-4" onSubmit={onSubmit} noValidate>
        <FormError message={formError} />
        <FormField label="Email" htmlFor="email" error={errors.email?.message}>
          <Input id="email" type="email" autoComplete="email" autoFocus={!prefilledEmail} aria-invalid={!!errors.email} {...register('email')} />
        </FormField>
        <FormField label="Password" htmlFor="password" error={errors.password?.message}>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            autoFocus={!!prefilledEmail}
            aria-invalid={!!errors.password}
            {...register('password')}
          />
        </FormField>
        <div className="-mt-1 text-right">
          <Link to="/forgot-password" className="text-sm text-primary hover:underline">
            Forgot your password?
          </Link>
        </div>
        <Button type="submit" size="lg" disabled={isSubmitting}>
          {isSubmitting ? <Loader2 className="animate-spin" /> : null}
          Sign in
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-muted-foreground">
        New to TaskFlow?{' '}
        <Link to="/register" className="font-medium text-primary hover:underline">
          Create an account
        </Link>
      </p>
    </div>
  )
}
