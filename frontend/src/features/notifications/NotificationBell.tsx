import { useState } from 'react'
import { Link } from 'react-router'
import { Bell, BellOff } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { ScrollArea } from '@/components/ui/scroll-area'
import { ErrorState, ListSkeleton } from '@/components/common/states'
import type { AppNotification } from '@/api/types'
import { NotificationItem } from './NotificationItem'
import { useMarkAllRead, useNotificationList, useOpenNotification, useUnreadCount } from './hooks'

export function NotificationBell() {
  const [open, setOpen] = useState(false)
  const unread = useUnreadCount()
  const list = useNotificationList()
  const markAllRead = useMarkAllRead()
  const openNotification = useOpenNotification()
  const count = unread.data?.count ?? 0
  const items = list.data?.pages.flatMap((page) => page.items).slice(0, 10) ?? []

  const handleOpen = (notification: AppNotification) => {
    setOpen(false)
    openNotification(notification)
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative" aria-label={`Notifications, ${count} unread`}>
          <Bell />
          {count > 0 ? (
            <span className="absolute -top-0.5 -right-0.5 flex min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold leading-4 text-primary-foreground">
              {count > 99 ? '99+' : count}
            </span>
          ) : null}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[min(24rem,calc(100vw-2rem))] p-0">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <p className="font-medium">Notifications</p>
          <Button
            variant="link"
            size="sm"
            className="h-auto p-0"
            disabled={count === 0 || markAllRead.isPending}
            onClick={() => markAllRead.mutate()}
          >
            Mark all as read
          </Button>
        </div>
        <ScrollArea className="max-h-[60vh]">
          <div className="p-1.5">
            {list.isPending ? (
              <ListSkeleton rows={3} className="p-2" />
            ) : list.isError ? (
              <ErrorState error={list.error} onRetry={() => list.refetch()} className="border-0" />
            ) : items.length === 0 ? (
              <div className="flex flex-col items-center gap-1 px-6 py-10 text-center">
                <BellOff className="mb-1 size-7 text-muted-foreground" aria-hidden />
                <p className="font-medium">You're all caught up!</p>
                <p className="text-sm text-muted-foreground">No new notifications.</p>
              </div>
            ) : (
              items.map((notification) => (
                <NotificationItem key={notification.id} notification={notification} onOpen={handleOpen} />
              ))
            )}
          </div>
        </ScrollArea>
        <div className="border-t p-2">
          <Button asChild variant="ghost" size="sm" className="w-full" onClick={() => setOpen(false)}>
            <Link to="/notifications">View all notifications</Link>
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  )
}
