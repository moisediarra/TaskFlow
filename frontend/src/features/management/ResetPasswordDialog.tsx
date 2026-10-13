import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { KeyRound, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { managementApi } from '@/api/endpoints'
import type { UserRow } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { FormError, FormField } from '@/components/common/FormField'
import { newPasswordField } from '@/features/auth/schemas'
import { applyFieldErrors, errorMessage } from '@/lib/errors'
import { queryKeys } from '@/lib/query-keys'

const schema = z
  .object({
    newPassword: newPasswordField,
    confirmPassword: z.string().min(1, 'Please confirm the new password.'),
    currentPassword: z.string().min(1, 'Enter your password to confirm.'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match.',
  })

type Values = z.infer<typeof schema>

interface ResetPasswordDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  user: Pick<UserRow, 'id' | 'name'>
}

/** The IT Manager sets a new password for someone who lost theirs; that person is signed out everywhere. */
export function ResetPasswordDialog({ open, onOpenChange, user }: ResetPasswordDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100svh-2rem)] overflow-y-auto sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <KeyRound className="size-5 text-amber-500" /> Reset {user.name}'s password?
          </DialogTitle>
          <DialogDescription>
            {user.name} will be signed out everywhere and will need the new password to sign in. Share it with them
            privately; they can change it from their profile.
          </DialogDescription>
        </DialogHeader>
        {/* Mounted only while open, so passwords never linger between openings. */}
        <ResetPasswordForm user={user} onClose={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function ResetPasswordForm({ user, onClose }: { user: Pick<UserRow, 'id' | 'name'>; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { newPassword: '', confirmPassword: '', currentPassword: '' },
  })

  const reset = useMutation({
    mutationFn: (values: Values) =>
      managementApi.resetPassword(user.id, values.newPassword, values.confirmPassword, values.currentPassword),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.management.all })
      toast.success(`${user.name}'s password was reset.`)
      onClose()
    },
    onError: (error) => {
      if (!applyFieldErrors(error, setError, ['newPassword', 'confirmPassword', 'currentPassword'])) {
        setFormError(errorMessage(error))
      }
    },
  })

  return (
    <form
      className="grid gap-4"
      onSubmit={handleSubmit((values) => {
        setFormError(null)
        reset.mutate(values)
      })}
      noValidate
    >
      <FormError message={formError} />
      <FormField label="New password" htmlFor="reset-new-password" error={errors.newPassword?.message} hint="At least 8 characters.">
        <Input
          id="reset-new-password"
          type="password"
          autoComplete="new-password"
          autoFocus
          aria-invalid={!!errors.newPassword}
          {...register('newPassword')}
        />
      </FormField>
      <FormField label="Confirm new password" htmlFor="reset-confirm-password" error={errors.confirmPassword?.message}>
        <Input
          id="reset-confirm-password"
          type="password"
          autoComplete="new-password"
          aria-invalid={!!errors.confirmPassword}
          {...register('confirmPassword')}
        />
      </FormField>
      <div className="border-t pt-4">
        <FormField
          label="Your password"
          htmlFor="reset-admin-password"
          error={errors.currentPassword?.message}
          hint="Confirms that you are making this change."
        >
          <Input
            id="reset-admin-password"
            type="password"
            autoComplete="current-password"
            aria-invalid={!!errors.currentPassword}
            {...register('currentPassword')}
          />
        </FormField>
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onClose} disabled={reset.isPending}>
          Cancel
        </Button>
        <Button type="submit" disabled={reset.isPending}>
          {reset.isPending ? <Loader2 className="animate-spin" /> : null}
          Reset password
        </Button>
      </DialogFooter>
    </form>
  )
}
