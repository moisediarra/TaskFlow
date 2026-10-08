import { NavLink, Outlet } from 'react-router'
import { cn } from '@/lib/utils'

const SECTIONS = [
  { to: '/management', label: 'Overview', end: true },
  { to: '/management/team-activity', label: 'Team Activity' },
  { to: '/management/workload', label: 'Workload' },
  { to: '/management/activity-logs', label: 'Activity Logs' },
  { to: '/management/users', label: 'Users' },
  { to: '/management/projects', label: 'Projects' },
]

/** IT Management section navigation (claude.md §5). */
export function ManagementLayout() {
  return (
    <div>
      <nav
        aria-label="Management"
        className="-mx-4 mb-6 flex gap-1 overflow-x-auto border-b px-4 sm:-mx-6 sm:px-6"
      >
        {SECTIONS.map(({ to, label, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              cn(
                '-mb-px shrink-0 border-b-2 border-transparent px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground',
                isActive && 'border-primary text-foreground',
              )
            }
          >
            {label}
          </NavLink>
        ))}
      </nav>
      <Outlet />
    </div>
  )
}
