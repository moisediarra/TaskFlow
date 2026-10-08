import { createBrowserRouter } from 'react-router'
import { AuthLayout } from '@/features/auth/AuthLayout'
import { ForgotPasswordPage } from '@/features/auth/ForgotPasswordPage'
import { RequireAuth, RequireGuest, RequireItManager } from '@/features/auth/guards'
import { LoginPage } from '@/features/auth/LoginPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { ResetPasswordPage } from '@/features/auth/ResetPasswordPage'
import { BoardPage } from '@/features/board/BoardPage'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { NotificationsPage } from '@/features/notifications/NotificationsPage'
import { ProfilePage } from '@/features/profile/ProfilePage'
import { ProjectLayout } from '@/features/projects/ProjectLayout'
import { ProjectOverviewPage } from '@/features/projects/ProjectOverviewPage'
import { ProjectSettingsPage } from '@/features/projects/ProjectSettingsPage'
import { ProjectsPage } from '@/features/projects/ProjectsPage'
import { NotFoundPage, RouteErrorPage } from '@/pages/NotFoundPage'
import { AppLayout } from './AppLayout'
import { HomeRedirect } from './HomeRedirect'
import { ManagementLayout } from './ManagementLayout'

// IT Management pages (and their charts) load on demand.
const management = {
  overview: async () => ({ Component: (await import('@/features/management/ManagementOverviewPage')).ManagementOverviewPage }),
  teamActivity: async () => ({ Component: (await import('@/features/management/TeamActivityPage')).TeamActivityPage }),
  workload: async () => ({ Component: (await import('@/features/management/WorkloadPage')).WorkloadPage }),
  activityLogs: async () => ({ Component: (await import('@/features/management/ActivityLogsPage')).ActivityLogsPage }),
  users: async () => ({ Component: (await import('@/features/management/UsersPage')).UsersPage }),
  user: async () => ({ Component: (await import('@/features/management/UserDetailPage')).UserDetailPage }),
  projects: async () => ({ Component: (await import('@/features/management/ManagementProjectsPage')).ManagementProjectsPage }),
}

export const router = createBrowserRouter([
  {
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <RequireGuest />,
        children: [
          {
            element: <AuthLayout />,
            children: [
              { path: '/login', element: <LoginPage /> },
              { path: '/register', element: <RegisterPage /> },
              { path: '/forgot-password', element: <ForgotPasswordPage /> },
              { path: '/reset-password', element: <ResetPasswordPage /> },
            ],
          },
        ],
      },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <AppLayout />,
            children: [
              { path: '/', element: <HomeRedirect /> },
              { path: '/dashboard', element: <DashboardPage /> },
              { path: '/projects', element: <ProjectsPage /> },
              {
                path: '/projects/:projectId',
                element: <ProjectLayout />,
                children: [
                  { index: true, element: <BoardPage /> },
                  { path: 'overview', element: <ProjectOverviewPage /> },
                  { path: 'settings', element: <ProjectSettingsPage /> },
                ],
              },
              { path: '/notifications', element: <NotificationsPage /> },
              { path: '/profile', element: <ProfilePage /> },
              {
                path: '/management',
                element: <RequireItManager />,
                children: [
                  {
                    element: <ManagementLayout />,
                    children: [
                      { index: true, lazy: management.overview },
                      { path: 'team-activity', lazy: management.teamActivity },
                      { path: 'workload', lazy: management.workload },
                      { path: 'activity-logs', lazy: management.activityLogs },
                      { path: 'users', lazy: management.users },
                      { path: 'users/:userId', lazy: management.user },
                      { path: 'projects', lazy: management.projects },
                    ],
                  },
                ],
              },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
])
