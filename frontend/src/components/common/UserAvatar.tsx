import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { initials } from '@/lib/format'
import { cn } from '@/lib/utils'

const TONES = [
  'bg-teal-100 text-teal-800',
  'bg-sky-100 text-sky-800',
  'bg-indigo-100 text-indigo-800',
  'bg-violet-100 text-violet-800',
  'bg-rose-100 text-rose-800',
  'bg-amber-100 text-amber-900',
  'bg-emerald-100 text-emerald-800',
]

function toneFor(seed: string) {
  let hash = 0
  for (const char of seed) hash = (hash * 31 + char.charCodeAt(0)) | 0
  return TONES[Math.abs(hash) % TONES.length]
}

interface UserAvatarProps {
  name: string
  id?: string
  className?: string
  size?: 'sm' | 'md' | 'lg'
}

/** Initials avatar with a stable color per person (no uploads in the MVP). */
export function UserAvatar({ name, id, className, size = 'md' }: UserAvatarProps) {
  const sizes = { sm: 'size-6 text-[10px]', md: 'size-8 text-xs', lg: 'size-12 text-base' }
  return (
    <Avatar className={cn(sizes[size], className)} title={name}>
      <AvatarFallback className={cn('font-medium', toneFor(id ?? name))}>{initials(name)}</AvatarFallback>
    </Avatar>
  )
}
