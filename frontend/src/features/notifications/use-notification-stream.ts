import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { accessTokenTtlSeconds, expireSession, getAccessToken, refreshSession } from '@/api/client'
import type { NotificationPush } from '@/api/types'
import { queryKeys } from '@/lib/query-keys'

/**
 * Receives notifications in real time over STOMP (claude.md stack: Spring WebSocket + STOMP), so a user
 * sees "You have been assigned…" without refreshing. Reconnects automatically with a fresh token.
 * When the server ends the connection because the person was signed out everywhere (password changed or reset,
 * account deactivated), the session is refreshed before reconnecting, or ended if it can't be.
 */
const SESSION_ENDED = 4001

export function useNotificationStream(enabled: boolean) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  useEffect(() => {
    if (!enabled) return
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    let rejected = false
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      reconnectDelay: 5_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      beforeConnect: async () => {
        if (rejected || accessTokenTtlSeconds() < 30) {
          rejected = false
          if (!(await refreshSession())) {
            await client.deactivate()
            expireSession()
            return
          }
        }
        client.connectHeaders = { Authorization: `Bearer ${getAccessToken() ?? ''}` }
      },
      // 4001: the server closed the connection because the session ended; an ERROR frame: CONNECT was refused.
      onWebSocketClose: (event) => {
        if (event.code === SESSION_ENDED) rejected = true
      },
      onStompError: () => {
        rejected = true
      },
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (message) => {
          const push = JSON.parse(message.body) as NotificationPush
          const { notification } = push
          queryClient.setQueryData(queryKeys.unreadCount, { count: push.unreadCount })
          void queryClient.invalidateQueries({ queryKey: queryKeys.notifications, exact: true })
          void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard })
          if (notification.projectId) {
            void queryClient.invalidateQueries({ queryKey: queryKeys.project(notification.projectId) })
          }
          if (notification.taskId) void queryClient.invalidateQueries({ queryKey: queryKeys.task(notification.taskId) })
          toast(`🔔 ${notification.title}`, {
            description: notification.message,
            action:
              notification.projectId && notification.taskId
                ? {
                    label: 'Open',
                    onClick: () => navigate(`/projects/${notification.projectId}?task=${notification.taskId}`),
                  }
                : undefined,
          })
        })
      },
    })
    client.activate()
    return () => {
      void client.deactivate()
    }
  }, [enabled, queryClient, navigate])
}
