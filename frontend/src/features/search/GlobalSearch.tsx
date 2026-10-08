import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { Activity as ActivityIcon, FolderKanban, Loader2, Search, UserRound } from 'lucide-react'
import { searchApi } from '@/api/endpoints'
import { Button } from '@/components/ui/button'
import {
  Command,
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command'
import { PriorityDot } from '@/features/tasks/badges'
import { STATUS_LABEL } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'
import { useDebouncedValue } from '@/lib/use-debounced-value'
import { timeAgo } from '@/lib/format'

/**
 * Debounced search (claude.md §19): tasks by title, description and tags for everyone; IT Managers also find
 * projects, people and activity. Open with the button or Ctrl/⌘+K.
 */
export function GlobalSearch({ global }: { global: boolean }) {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [text, setText] = useState('')
  const query = useDebouncedValue(text.trim(), 300)
  const enabled = open && query.length >= 2
  const results = useQuery({
    queryKey: queryKeys.search(query),
    queryFn: () => searchApi.search(query),
    enabled,
    staleTime: 30_000,
  })

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key.toLowerCase() === 'k' && (event.metaKey || event.ctrlKey)) {
        event.preventDefault()
        setOpen((current) => !current)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  const go = (path: string) => {
    setOpen(false)
    setText('')
    navigate(path)
  }

  const data = enabled ? results.data : undefined
  const nothing =
    data &&
    data.tasks.length === 0 &&
    !data.projects?.length &&
    !data.users?.length &&
    !data.activities?.length

  return (
    <>
      <Button
        variant="outline"
        className="size-8 justify-center gap-2 px-0 text-muted-foreground sm:w-56 sm:justify-start sm:px-2.5 lg:w-72"
        onClick={() => setOpen(true)}
        aria-label="Search"
      >
        <Search />
        <span className="hidden sm:inline">{global ? 'Search everything…' : 'Search tasks…'}</span>
        <kbd className="ml-auto hidden rounded border bg-muted px-1.5 font-mono text-[10px] sm:inline">Ctrl K</kbd>
      </Button>
      <CommandDialog
        open={open}
        onOpenChange={setOpen}
        title="Search"
        description={global ? 'Search tasks, projects, people and activity' : 'Search tasks by title, description or tag'}
      >
        <Command shouldFilter={false}>
          <CommandInput
            placeholder={global ? 'Search tasks, projects, people, activity…' : 'Search tasks by title, description or tag…'}
            value={text}
            onValueChange={setText}
          />
          <CommandList className="max-h-[60vh]">
            {text.trim().length < 2 ? (
              <p className="px-3 py-6 text-center text-sm text-muted-foreground">Type at least 2 characters.</p>
            ) : results.isFetching && !data ? (
              <p className="flex items-center justify-center gap-2 px-3 py-6 text-sm text-muted-foreground">
                <Loader2 className="size-4 animate-spin" /> Searching…
              </p>
            ) : results.isError ? (
              <p className="px-3 py-6 text-center text-sm text-muted-foreground">Search is unavailable right now.</p>
            ) : nothing ? (
              <CommandEmpty>No results for “{query}”.</CommandEmpty>
            ) : data ? (
              <>
                {data.tasks.length > 0 ? (
                  <CommandGroup heading="Tasks">
                    {data.tasks.map((task) => (
                      <CommandItem key={task.id} value={`task-${task.id}`} onSelect={() => go(`/projects/${task.projectId}?task=${task.id}`)}>
                        <PriorityDot priority={task.priority} />
                        <span className="min-w-0 flex-1 truncate">{task.title}</span>
                        <span className="shrink-0 text-xs text-muted-foreground">
                          {task.projectName} · {STATUS_LABEL[task.status]}
                        </span>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                ) : null}
                {data.projects?.length ? (
                  <CommandGroup heading="Projects">
                    {data.projects.map((project) => (
                      <CommandItem key={project.id} value={`project-${project.id}`} onSelect={() => go(`/projects/${project.id}/overview`)}>
                        <FolderKanban />
                        <span className="truncate">{project.name}</span>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                ) : null}
                {data.users?.length ? (
                  <CommandGroup heading="People">
                    {data.users.map((user) => (
                      <CommandItem key={user.id} value={`user-${user.id}`} onSelect={() => go(`/management/users/${user.id}`)}>
                        <UserRound />
                        <span className="truncate">{user.name}</span>
                        <span className="ml-auto truncate text-xs text-muted-foreground">{user.email}</span>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                ) : null}
                {data.activities?.length ? (
                  <CommandGroup heading="Activity">
                    {data.activities.map((activity) => (
                      <CommandItem
                        key={activity.id}
                        value={`activity-${activity.id}`}
                        onSelect={() =>
                          go(activity.projectId ? `/projects/${activity.projectId}/overview` : '/management/activity-logs')
                        }
                      >
                        <ActivityIcon />
                        <span className="min-w-0 flex-1 truncate">{activity.description}</span>
                        <span className="shrink-0 text-xs text-muted-foreground">{timeAgo(activity.createdAt)}</span>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                ) : null}
              </>
            ) : null}
          </CommandList>
        </Command>
      </CommandDialog>
    </>
  )
}
