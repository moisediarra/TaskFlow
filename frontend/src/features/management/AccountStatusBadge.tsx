import type { UserStatus } from '@/api/types'
import { cn } from '@/lib/utils'

export function AccountStatusBadge({ status }: { status: UserStatus }) {
  const active = status === 'ACTIVE'
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium',
        active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600',
      )}
    >
      <span className={cn('size-1.5 rounded-full', active ? 'bg-emerald-500' : 'bg-slate-400')} aria-hidden />
      {active ? 'Active' : 'Deactivated'}
    </span>
  )
}
