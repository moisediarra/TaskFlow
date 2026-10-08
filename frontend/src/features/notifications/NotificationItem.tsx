import { Check } from 'lucide-react'
import type { AppNotification } from '@/api/types'
import { timeAgo } from '@/lib/format'
import { cn } from '@/lib/utils'

/** One row of the notification panel: unread items carry a dot, read ones a check (claude.md §21). */
export function NotificationItem({
  notification,
  onOpen,
}: {
  notification: AppNotification
  onOpen: (notification: AppNotification) => void
}) {
  return (
    <button
      type="button"
      onClick={() => onOpen(notification)}
      className={cn(
        'flex w-full gap-3 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-muted focus-visible:bg-muted focus-visible:outline-none',
        !notification.read && 'bg-accent/40',
      )}
    >
      <span className="mt-1.5 flex size-4 shrink-0 items-center justify-center" aria-hidden>
        {notification.read ? (
          <Check className="size-3.5 text-muted-foreground" />
        ) : (
          <span className="size-2 rounded-full bg-primary" />
        )}
      </span>
      <span className="min-w-0 flex-1">
        <span className={cn('block text-sm', notification.read ? 'text-foreground/80' : 'font-medium')}>
          {notification.title}
          {notification.read ? null : <span className="sr-only"> (unread)</span>}
        </span>
        <span className="mt-0.5 block text-sm text-muted-foreground line-clamp-2">{notification.message}</span>
        <span className="mt-1 block text-xs text-muted-foreground">{timeAgo(notification.createdAt)}</span>
      </span>
    </button>
  )
}
