import { useState, type ReactNode } from 'react'
import { Loader2, ShieldAlert } from 'lucide-react'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { FormError, FormField } from '@/components/common/FormField'
import { errorMessage } from '@/lib/errors'

interface ConfirmWithPasswordDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description: ReactNode
  confirmLabel: string
  destructive?: boolean
  onConfirm: (password: string) => Promise<unknown>
}

/**
 * Explicit authorization for sensitive account changes (claude.md §31): the IT Manager confirms and
 * re-enters their password; the backend verifies it again.
 */
export function ConfirmWithPasswordDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  destructive = false,
  onConfirm,
}: ConfirmWithPasswordDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <ShieldAlert className="size-5 text-amber-500" /> {title}
          </DialogTitle>
          <DialogDescription asChild>
            <div className="text-sm text-muted-foreground">{description}</div>
          </DialogDescription>
        </DialogHeader>
        {/* Mounted only while open, so the password never lingers between openings. */}
        <PasswordConfirmForm
          confirmLabel={confirmLabel}
          destructive={destructive}
          onCancel={() => onOpenChange(false)}
          onConfirm={async (password) => {
            await onConfirm(password)
            onOpenChange(false)
          }}
        />
      </DialogContent>
    </Dialog>
  )
}

function PasswordConfirmForm({
  confirmLabel,
  destructive,
  onCancel,
  onConfirm,
}: {
  confirmLabel: string
  destructive: boolean
  onCancel: () => void
  onConfirm: (password: string) => Promise<void>
}) {
  const [password, setPassword] = useState('')
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  const submit = async () => {
    if (!password) {
      setFieldError('Enter your password to confirm.')
      return
    }
    setPending(true)
    setFieldError(null)
    setFormError(null)
    try {
      await onConfirm(password)
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors.currentPassword) setFieldError(error.fieldErrors.currentPassword)
      else setFormError(errorMessage(error))
      setPending(false)
    }
  }

  return (
    <form
      className="grid gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        void submit()
      }}
      noValidate
    >
      <FormError message={formError} />
      <FormField label="Your password" htmlFor="confirm-password" error={fieldError ?? undefined}>
        <Input
          id="confirm-password"
          type="password"
          autoComplete="current-password"
          autoFocus
          value={password}
          aria-invalid={!!fieldError}
          onChange={(event) => setPassword(event.target.value)}
        />
      </FormField>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onCancel} disabled={pending}>
          Cancel
        </Button>
        <Button type="submit" variant={destructive ? 'destructive' : 'default'} disabled={pending}>
          {pending ? <Loader2 className="animate-spin" /> : null}
          {confirmLabel}
        </Button>
      </DialogFooter>
    </form>
  )
}
