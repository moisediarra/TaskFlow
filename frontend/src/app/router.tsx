import { createBrowserRouter } from 'react-router'
import { AuthLayout } from '@/features/auth/AuthLayout'
import { ForgotPasswordPage } from '@/features/auth/ForgotPasswordPage'
import { RequireAuth, RequireGuest, RequireItManager } from '@/features/auth/guards'
import { LoginPage } from '@/features/auth/LoginPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { ResetPasswordPage } from '@/features/auth/ResetPasswordPage'
import { NotFoundPage, RouteErrorPage } from '@/pages/NotFoundPage'

// The sign-in pages load eagerly; the signed-in app and its pages load on demand, so the login screen stays light.
const page = {
  appLayout: async () => ({ Component: (await import('./AppLayout')).AppLayout }),
  home: async () => ({ Component: (await import('./HomeRedirect')).HomeRedirect }),
  dashboard: async () => ({ Component: (await import('@/features/dashboard/DashboardPage')).DashboardPage }),
  projects: async () => ({ Component: (await import('@/features/projects/ProjectsPage')).ProjectsPage }),
  projectLayout: async () => ({ Component: (await import('@/features/projects/ProjectLayout')).ProjectLayout }),
  board: async () => ({ Component: (await import('@/features/board/BoardPage')).BoardPage }),
  projectOverview: async () => ({ Component: (await import('@/features/projects/ProjectOverviewPage')).ProjectOverviewPage }),
  projectSettings: async () => ({ Component: (await import('@/features/projects/ProjectSettingsPage')).ProjectSettingsPage }),
  notifications: async () => ({ Component: (await import('@/features/notifications/NotificationsPage')).NotificationsPage }),
  profile: async () => ({ Component: (await import('@/features/profile/ProfilePage')).ProfilePage }),
  managementLayout: async () => ({ Component: (await import('./ManagementLayout')).ManagementLayout }),
  overview: async () => ({ Component: (await import('@/features/management/ManagementOverviewPage')).ManagementOverviewPage }),
  teamActivity: async () => ({ Component: (await import('@/features/management/TeamActivityPage')).TeamActivityPage }),
  workload: async () => ({ Component: (await import('@/features/management/WorkloadPage')).WorkloadPage }),
  activityLogs: async () => ({ Component: (await import('@/features/management/ActivityLogsPage')).ActivityLogsPage }),
  users: async () => ({ Component: (await import('@/features/management/UsersPage')).UsersPage }),
  user: async () => ({ Component: (await import('@/features/management/UserDetailPage')).UserDetailPage }),
  managementProjects: async () => ({ Component: (await import('@/features/management/ManagementProjectsPage')).ManagementProjectsPage }),
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
            lazy: page.appLayout,
            children: [
              { path: '/', lazy: page.home },
              { path: '/dashboard', lazy: page.dashboard },
              { path: '/projects', lazy: page.projects },
              {
                path: '/projects/:projectId',
                lazy: page.projectLayout,
                children: [
                  { index: true, lazy: page.board },
                  { path: 'overview', lazy: page.projectOverview },
                  { path: 'settings', lazy: page.projectSettings },
                ],
              },
              { path: '/notifications', lazy: page.notifications },
              { path: '/profile', lazy: page.profile },
              {
                path: '/management',
                element: <RequireItManager />,
                children: [
                  {
                    lazy: page.managementLayout,
                    children: [
                      { index: true, lazy: page.overview },
                      { path: 'team-activity', lazy: page.teamActivity },
                      { path: 'workload', lazy: page.workload },
                      { path: 'activity-logs', lazy: page.activityLogs },
                      { path: 'users', lazy: page.users },
                      { path: 'users/:userId', lazy: page.user },
                      { path: 'projects', lazy: page.managementProjects },
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
