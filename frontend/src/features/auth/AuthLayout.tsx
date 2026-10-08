import { Outlet } from 'react-router'
import { Logo } from '@/components/common/Logo'

const PREVIEW = [
  { title: 'Backlog', cards: ['Improve notifications', 'Add dark mode'] },
  { title: 'In Progress', cards: ['Authentication API', 'Login UI'] },
  { title: 'Done', cards: ['Payment integration'] },
]

/** Split layout for the signed-out pages: brand panel on large screens, form on the right. */
export function AuthLayout() {
  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <aside className="relative hidden flex-col justify-between overflow-hidden bg-primary p-10 text-primary-foreground lg:flex">
        <Logo className="[&_rect:first-child]:fill-white/15" />
        <div className="space-y-6">
          <h2 className="max-w-md text-3xl font-semibold leading-tight">
            Simple projects, a clear board, and everyone knowing who does what.
          </h2>
          <div className="grid max-w-lg grid-cols-3 gap-3" aria-hidden>
            {PREVIEW.map((column) => (
              <div key={column.title} className="rounded-xl bg-white/10 p-3">
                <p className="mb-2 text-xs font-medium text-primary-foreground/80">{column.title}</p>
                <div className="space-y-2">
                  {column.cards.map((card) => (
                    <div key={card} className="rounded-lg bg-white/90 px-2.5 py-2 text-xs font-medium text-slate-800 shadow-sm">
                      {card}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
        <p className="text-sm text-primary-foreground/70">Projects · Kanban · Task tracking · Team visibility</p>
      </aside>
      <main className="flex items-center justify-center px-4 py-10 sm:px-8">
        <div className="w-full max-w-sm">
          <Logo className="mb-8 lg:hidden" />
          <Outlet />
        </div>
      </main>
    </div>
  )
}
