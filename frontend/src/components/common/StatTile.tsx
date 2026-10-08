import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface StatTileProps {
  label: string
  value: number | string
  hint?: ReactNode
  icon?: ReactNode
  tone?: 'default' | 'warning' | 'danger' | 'success'
  className?: string
}

const toneClasses = {
  default: 'text-foreground',
  warning: 'text-amber-700',
  danger: 'text-red-700',
  success: 'text-emerald-700',
}

/** A single metric (dashboard tiles, claude.md §6 and §24). */
export function StatTile({ label, value, hint, icon, tone = 'default', className }: StatTileProps) {
  return (
    <div className={cn('rounded-xl border bg-card p-4 shadow-xs', className)}>
      <div className="flex items-center justify-between gap-2 text-sm text-muted-foreground">
        <span>{label}</span>
        {icon ? <span className="[&_svg]:size-4">{icon}</span> : null}
      </div>
      <p className={cn('mt-2 text-3xl font-semibold tabular-nums tracking-tight', toneClasses[tone])}>{value}</p>
      {hint ? <p className="mt-1 text-xs text-muted-foreground">{hint}</p> : null}
    </div>
  )
}
