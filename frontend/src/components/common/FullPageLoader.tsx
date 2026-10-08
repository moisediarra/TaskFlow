import { Loader2 } from 'lucide-react'

export function FullPageLoader({ label = 'Loading TaskFlow…' }: { label?: string }) {
  return (
    <div className="grid min-h-svh place-items-center" role="status" aria-live="polite">
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden />
        {label}
      </div>
    </div>
  )
}
