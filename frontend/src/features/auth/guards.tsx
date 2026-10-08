import { Navigate, Outlet, useLocation } from 'react-router'
import { FullPageLoader } from '@/components/common/FullPageLoader'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { homePath } from './auth-context'
import { useAuth } from './use-auth'

/** Signed-in area; anonymous visitors are sent to the login page and brought back afterwards. */
export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()
  if (status === 'loading') return <FullPageLoader />
  if (status === 'anonymous') {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  return <Outlet />
}

/** Login, registration and password pages are for signed-out visitors only. */
export function RequireGuest() {
  const { status, user } = useAuth()
  if (status === 'loading') return <FullPageLoader />
  if (status === 'authenticated' && user) return <Navigate to={homePath(user)} replace />
  return <Outlet />
}

/** IT Management pages (the backend enforces the same rule). */
export function RequireItManager() {
  const { user } = useAuth()
  if (user?.role !== 'IT_MANAGER') return <ForbiddenPage />
  return <Outlet />
}
