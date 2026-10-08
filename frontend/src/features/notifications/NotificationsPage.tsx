import { BellOff, CheckCheck, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/common/PageHeader'
import { EmptyState, ErrorState, ListSkeleton } from '@/components/common/states'
import { NotificationItem } from './NotificationItem'
import { useMarkAllRead, useNotificationList, useOpenNotification, useUnreadCount } from './hooks'

export function NotificationsPage() {
  const list = useNotificationList()
  const unread = useUnreadCount()
  const markAllRead = useMarkAllRead()
  const openNotification = useOpenNotification()
  const items = list.data?.pages.flatMap((page) => page.items) ?? []
  const count = unread.data?.count ?? 0

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Notifications"
        description={count > 0 ? `${count} unread` : 'Updates about tasks that concern you.'}
        actions={
          <Button variant="outline" disabled={count === 0 || markAllRead.isPending} onClick={() => markAllRead.mutate()}>
            <CheckCheck /> Mark all as read
          </Button>
        }
      />
      {list.isPending ? (
        <ListSkeleton rows={5} />
      ) : list.isError ? (
        <ErrorState error={list.error} onRetry={() => list.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState icon={<BellOff />} title="You're all caught up!" description="No new notifications." />
      ) : (
        <div className="rounded-xl border bg-card p-1.5 shadow-xs">
          {items.map((notification) => (
            <NotificationItem key={notification.id} notification={notification} onOpen={openNotification} />
          ))}
          {list.hasNextPage ? (
            <div className="p-2">
              <Button
                variant="ghost"
                className="w-full"
                disabled={list.isFetchingNextPage}
                onClick={() => list.fetchNextPage()}
              >
                {list.isFetchingNextPage ? <Loader2 className="animate-spin" /> : null}
                Load older notifications
              </Button>
            </div>
          ) : null}
        </div>
      )}
    </div>
  )
}
