import { useState, type KeyboardEvent } from 'react'
import { Plus, X } from 'lucide-react'
import type { Tag } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { TAG_COLORS } from '@/lib/labels'
import { cn } from '@/lib/utils'

interface TagPickerProps {
  id: string
  available: Tag[]
  selectedIds: string[]
  newTags: string[]
  /** Only project owners can create tags; others pick from existing ones. */
  canCreate: boolean
  onChange: (selectedIds: string[], newTags: string[]) => void
}

/** Toggle project tags on a task, and create new ones inline (claude.md §17). */
export function TagPicker({ id, available, selectedIds, newTags, canCreate, onChange }: TagPickerProps) {
  const [draft, setDraft] = useState('')
  const [hint, setHint] = useState<string | null>(null)

  const toggle = (tagId: string) => {
    onChange(selectedIds.includes(tagId) ? selectedIds.filter((value) => value !== tagId) : [...selectedIds, tagId], newTags)
  }

  const addDraft = () => {
    const name = draft.trim().replace(/\s+/g, ' ')
    if (!name) return
    if (name.length > 30) {
      setHint('Tag names must be at most 30 characters.')
      return
    }
    const existing = available.find((tag) => tag.name.toLowerCase() === name.toLowerCase())
    if (existing) {
      if (!selectedIds.includes(existing.id)) onChange([...selectedIds, existing.id], newTags)
    } else if (!newTags.some((tag) => tag.toLowerCase() === name.toLowerCase())) {
      onChange(selectedIds, [...newTags, name])
    }
    setDraft('')
    setHint(null)
  }

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      event.preventDefault()
      addDraft()
    }
  }

  return (
    <div className="grid gap-2">
      {available.length === 0 && newTags.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {canCreate ? 'No tags yet in this project. Add the first one below.' : 'No tags in this project yet.'}
        </p>
      ) : (
        <div className="flex flex-wrap gap-1.5" role="group" aria-label="Tags">
          {available.map((tag) => {
            const selected = selectedIds.includes(tag.id)
            return (
              <button
                key={tag.id}
                type="button"
                aria-pressed={selected}
                onClick={() => toggle(tag.id)}
                className={cn(
                  'rounded-md px-2 py-1 text-xs font-medium ring-1 ring-inset transition-colors',
                  selected ? (TAG_COLORS[tag.color] ?? TAG_COLORS.slate) : 'bg-background text-muted-foreground ring-border hover:bg-muted',
                )}
              >
                {tag.name}
              </button>
            )
          })}
          {newTags.map((name) => (
            <span key={name} className="inline-flex items-center gap-1 rounded-md bg-accent px-2 py-1 text-xs font-medium text-accent-foreground">
              {name}
              <span className="text-[10px] uppercase opacity-70">new</span>
              <button
                type="button"
                aria-label={`Remove ${name}`}
                onClick={() => onChange(selectedIds, newTags.filter((tag) => tag !== name))}
                className="rounded hover:bg-black/5"
              >
                <X className="size-3" />
              </button>
            </span>
          ))}
        </div>
      )}
      {canCreate ? (
        <div className="flex gap-2">
          <Input
            id={id}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={onKeyDown}
            placeholder="Add a tag (e.g. Backend)"
            maxLength={40}
            className="h-8"
          />
          <Button type="button" variant="outline" size="sm" className="h-8" onClick={addDraft} disabled={!draft.trim()}>
            <Plus /> Add
          </Button>
        </div>
      ) : null}
      {hint ? <p className="text-xs text-destructive">{hint}</p> : null}
    </div>
  )
}
