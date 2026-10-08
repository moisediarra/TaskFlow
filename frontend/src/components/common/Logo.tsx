import { cn } from '@/lib/utils'

/** TaskFlow mark: three columns of decreasing height, i.e. work flowing towards done. */
export function Logo({ className, withName = true }: { className?: string; withName?: boolean }) {
  return (
    <span className={cn('inline-flex items-center gap-2 font-semibold tracking-tight', className)}>
      <svg viewBox="0 0 32 32" className="size-7 shrink-0" aria-hidden>
        <rect width="32" height="32" rx="8" className="fill-primary" />
        <rect x="7" y="8" width="5" height="16" rx="2" fill="#ccfbf1" />
        <rect x="13.5" y="8" width="5" height="11" rx="2" fill="#5eead4" />
        <rect x="20" y="8" width="5" height="7" rx="2" fill="#ffffff" />
      </svg>
      {withName ? <span className="text-lg">TaskFlow</span> : <span className="sr-only">TaskFlow</span>}
    </span>
  )
}
