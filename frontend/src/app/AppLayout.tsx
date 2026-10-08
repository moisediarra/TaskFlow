import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { Bell, LayoutDashboard, LogOut, Menu, ShieldCheck, SquareKanban, UserRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { Logo } from '@/components/common/Logo'
import { UserAvatar } from '@/components/common/UserAvatar'
import { homePath } from '@/features/auth/auth-context'
import { useAuth, useCurrentUser } from '@/features/auth/use-auth'
import { NotificationBell } from '@/features/notifications/NotificationBell'
import { useNotificationStream } from '@/features/notifications/use-notification-stream'
import { GlobalSearch } from '@/features/search/GlobalSearch'
import { ROLE_LABEL } from '@/lib/labels'
import { cn } from '@/lib/utils'

/** Signed-in shell: Dashboard · Projects · (Management) · search · 🔔 · profile menu (claude.md §5). */
export function AppLayout() {
  const user = useCurrentUser()
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const isItManager = user.role === 'IT_MANAGER'
  useNotificationStream(true)

  const links = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/projects', label: 'Projects', icon: SquareKanban },
    ...(isItManager ? [{ to: '/management', label: 'Management', icon: ShieldCheck }] : []),
  ]

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-svh">
      <header className="sticky top-0 z-40 border-b bg-background/90 backdrop-blur supports-[backdrop-filter]:bg-background/75">
        <div className="mx-auto flex h-14 max-w-7xl items-center gap-3 px-4 sm:px-6">
          <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open menu">
                <Menu />
              </Button>
            </SheetTrigger>
            <SheetContent side="left" className="w-72">
              <SheetHeader>
                <SheetTitle>
                  <Logo />
                </SheetTitle>
              </SheetHeader>
              <nav className="grid gap-1 px-4" aria-label="Main">
                {[...links, { to: '/notifications', label: 'Notifications', icon: Bell }, { to: '/profile', label: 'Profile', icon: UserRound }].map(
                  ({ to, label, icon: Icon }) => (
                    <NavLink
                      key={to}
                      to={to}
                      onClick={() => setMenuOpen(false)}
                      className={({ isActive }) =>
                        cn(
                          'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors hover:bg-muted',
                          isActive && 'bg-accent text-accent-foreground',
                        )
                      }
                    >
                      <Icon className="size-4" /> {label}
                    </NavLink>
                  ),
                )}
                <button
                  type="button"
                  onClick={handleLogout}
                  className="flex items-center gap-3 rounded-lg px-3 py-2 text-left text-sm font-medium text-destructive hover:bg-muted"
                >
                  <LogOut className="size-4" /> Log out
                </button>
              </nav>
            </SheetContent>
          </Sheet>

          <Link to={homePath(user)} aria-label="TaskFlow home">
            <Logo className="hidden sm:inline-flex" />
            <Logo withName={false} className="sm:hidden" />
          </Link>

          <nav className="ml-4 hidden items-center gap-1 md:flex" aria-label="Main">
            {links.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground',
                    isActive && 'bg-accent text-accent-foreground hover:bg-accent',
                  )
                }
              >
                <Icon className="size-4" /> {label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex min-w-0 items-center gap-1.5">
            <GlobalSearch global={isItManager} />
            <NotificationBell />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="h-9 gap-2 px-1.5" aria-label="Account menu">
                  <UserAvatar name={user.name} id={user.id} />
                  <span className="hidden max-w-32 truncate text-sm font-medium lg:inline">{user.name}</span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>
                  <p className="truncate font-medium">{user.name}</p>
                  <p className="truncate text-xs font-normal text-muted-foreground">{ROLE_LABEL[user.role]}</p>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onSelect={() => navigate('/profile')}>
                  <UserRound /> Profile
                </DropdownMenuItem>
                <DropdownMenuItem onSelect={() => navigate('/notifications')}>
                  <Bell /> Notifications
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem variant="destructive" onSelect={handleLogout}>
                  <LogOut /> Log out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 sm:py-8">
        <Outlet />
      </main>
    </div>
  )
}
