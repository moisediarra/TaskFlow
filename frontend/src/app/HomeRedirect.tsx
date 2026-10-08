import { Navigate } from 'react-router'
import { homePath } from '@/features/auth/auth-context'
import { useCurrentUser } from '@/features/auth/use-auth'

/** "/" sends IT Managers to Management and everyone else to their dashboard (claude.md §4). */
export function HomeRedirect() {
  return <Navigate to={homePath(useCurrentUser())} replace />
}
