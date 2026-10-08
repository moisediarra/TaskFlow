import { createContext } from 'react'
import type { Profile, Session } from '@/api/types'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export interface AuthContextValue {
  status: AuthStatus
  user: Profile | null
  login: (email: string, password: string) => Promise<Profile>
  logout: () => Promise<void>
  /** Adopts a session returned by the API (e.g. after a password change). */
  startSession: (session: Session) => void
  updateUser: (profile: Profile) => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

/** Where a user lands after signing in (claude.md §4). */
export function homePath(user: Pick<Profile, 'role'>): string {
  return user.role === 'IT_MANAGER' ? '/management' : '/dashboard'
}

/** Only same-site relative paths are accepted as a post-login redirect (no open redirects). */
export function safeNext(next: string | null): string | null {
  if (!next || !next.startsWith('/') || next.startsWith('//') || next.startsWith('/\\')) return null
  return next
}
