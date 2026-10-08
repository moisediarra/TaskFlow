import { useCallback } from 'react'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient, type InfiniteData } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { notificationsApi } from '@/api/endpoints'
import type { AppNotification, CursorPage } from '@/api/types'
import { queryKeys } from '@/lib/query-keys'

type NotificationPages = InfiniteData<CursorPage<AppNotification>, string | null>

export function useUnreadCount() {
  return useQuery({
    queryKey: queryKeys.unreadCount,
    queryFn: notificationsApi.unreadCount,
    // Real-time pushes keep this fresh; polling is only a fallback when the socket is down.
    refetchInterval: 120_000,
  })
}

export function useNotificationList() {
  return useInfiniteQuery({
    queryKey: queryKeys.notifications,
    queryFn: ({ pageParam }) => notificationsApi.list(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  })
}

function markInCache(pages: NotificationPages | undefined, predicate: (n: AppNotification) => boolean) {
  if (!pages) return pages
  return {
    ...pages,
    pages: pages.pages.map((page) => ({
      ...page,
      items: page.items.map((item) => (predicate(item) ? { ...item, read: true } : item)),
    })),
  }
}

export function useMarkRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => notificationsApi.markRead(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.notifications })
      queryClient.setQueryData<NotificationPages>(queryKeys.notifications, (pages) => markInCache(pages, (n) => n.id === id))
      queryClient.setQueryData<{ count: number }>(queryKeys.unreadCount, (current) =>
        current ? { count: Math.max(0, current.count - 1) } : current,
      )
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: queryKeys.notifications }),
  })
}

export function useMarkAllRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: notificationsApi.markAllRead,
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: queryKeys.notifications })
      queryClient.setQueryData<NotificationPages>(queryKeys.notifications, (pages) => markInCache(pages, () => true))
      queryClient.setQueryData(queryKeys.unreadCount, { count: 0 })
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: queryKeys.notifications }),
  })
}

/** Marks the notification read and opens the related task (claude.md §21). */
export function useOpenNotification() {
  const navigate = useNavigate()
  const markRead = useMarkRead()
  return useCallback(
    (notification: AppNotification) => {
      if (!notification.read) markRead.mutate(notification.id)
      if (notification.projectId && notification.taskId) {
        navigate(`/projects/${notification.projectId}?task=${notification.taskId}`)
      } else if (notification.projectId) {
        navigate(`/projects/${notification.projectId}`)
      }
    },
    [markRead, navigate],
  )
}
