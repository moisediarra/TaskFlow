import type { ReactNode } from 'react'
import { AlertTriangle, RefreshCw, WifiOff } from 'lucide-react'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  description?: string
  action?: ReactNode
  className?: string
}

/** Useful empty states everywhere (claude.md §36). */
export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed bg-card/60 px-6 py-10 text-center',
        className,
      )}
    >
      {icon ? <div className="mb-1 text-muted-foreground [&_svg]:size-8">{icon}</div> : null}
      <p className="font-medium">{title}</p>
      {description ? <p className="max-w-sm text-sm text-muted-foreground">{description}</p> : null}
      {action ? <div className="mt-3">{action}</div> : null}
    </div>
  )
}

/** Friendly error with a retry button; never shows technical details (claude.md §37). */
export function ErrorState({ error, onRetry, className }: { error: unknown; onRetry?: () => void; className?: string }) {
  const network = error instanceof ApiError && error.isNetworkError
  const forbidden = error instanceof ApiError && error.status === 403
  const notFound = error instanceof ApiError && error.status === 404
  const title = network
    ? "Can't reach TaskFlow"
    : forbidden
      ? "You don't have access to this"
      : notFound
        ? 'Not found'
        : 'Something went wrong'
  const description =
    error instanceof ApiError ? error.message : 'An unexpected error occurred. Please try again in a moment.'
  return (
    <div
      role="alert"
      className={cn('flex flex-col items-center gap-2 rounded-xl border bg-card px-6 py-10 text-center', className)}
    >
      {network ? <WifiOff className="size-7 text-muted-foreground" aria-hidden /> : <AlertTriangle className="size-7 text-amber-500" aria-hidden />}
      <p className="font-medium">{title}</p>
      <p className="max-w-md text-sm text-muted-foreground">{description}</p>
      {onRetry && !forbidden && !notFound ? (
        <Button variant="outline" size="sm" className="mt-2" onClick={onRetry}>
          <RefreshCw /> Try again
        </Button>
      ) : null}
    </div>
  )
}

export function ListSkeleton({ rows = 4, className }: { rows?: number; className?: string }) {
  return (
    <div className={cn('grid gap-2', className)} aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }, (_, index) => (
        <Skeleton key={index} className="h-12 w-full rounded-lg" />
      ))}
    </div>
  )
}
