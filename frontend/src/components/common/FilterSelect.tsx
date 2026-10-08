import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

export const ANY = 'any'

interface FilterSelectProps {
  id: string
  label: string
  value: string | undefined
  options: { value: string; label: string }[]
  anyLabel?: string
  onChange: (value: string | undefined) => void
  className?: string
}

/** Compact labelled select for filter rows; "any" clears the filter. */
export function FilterSelect({ id, label, value, options, anyLabel = 'Any', onChange, className }: FilterSelectProps) {
  return (
    <div className={className ?? 'grid min-w-36 gap-1'}>
      <Label htmlFor={id} className="text-xs text-muted-foreground">
        {label}
      </Label>
      <Select value={value ?? ANY} onValueChange={(next) => onChange(next === ANY ? undefined : next)}>
        <SelectTrigger id={id} className="w-full bg-card">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ANY}>{anyLabel}</SelectItem>
          {options.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
