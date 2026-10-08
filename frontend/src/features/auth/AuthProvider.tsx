import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { onSessionExpired, refreshSession, setAccessToken } from '@/api/client'
import { authApi } from '@/api/endpoints'
import type { Profile, Session } from '@/api/types'
import { AuthContext, type AuthContextValue, type AuthStatus } from './auth-context'

interface AuthState {
  status: AuthStatus
  user: Profile | null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [state, setState] = useState<AuthState>({ status: 'loading', user: null })

  // Restore the session from the refresh cookie on first load.
  useEffect(() => {
    let cancelled = false
    refreshSession().then((session) => {
      if (!cancelled) setState(session ? { status: 'authenticated', user: session.user } : { status: 'anonymous', user: null })
    })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    onSessionExpired(() => {
      setAccessToken(null)
      queryClient.clear()
      setState((current) => {
        if (current.status === 'authenticated') toast.info('Your session has expired. Please sign in again.')
        return { status: 'anonymous', user: null }
      })
    })
  }, [queryClient])

  const startSession = useCallback((session: Session) => {
    setAccessToken(session.accessToken)
    setState({ status: 'authenticated', user: session.user })
  }, [])

  const login = useCallback(
    async (email: string, password: string) => {
      const session = await authApi.login({ email, password })
      queryClient.clear()
      startSession(session)
      return session.user
    },
    [queryClient, startSession],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      setAccessToken(null)
      queryClient.clear()
      setState({ status: 'anonymous', user: null })
    }
  }, [queryClient])

  const updateUser = useCallback((profile: Profile) => {
    setState((current) => ({ ...current, user: profile }))
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, login, logout, startSession, updateUser }),
    [state, login, logout, startSession, updateUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
