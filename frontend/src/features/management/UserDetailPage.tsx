import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { ChevronLeft, ShieldCheck, UserCheck, UserX } from 'lucide-react'
import { toast } from 'sonner'
import { managementApi } from '@/api/endpoints'
import type { Role } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { StatTile } from '@/components/common/StatTile'
import { ErrorState } from '@/components/common/states'
import { UserAvatar } from '@/components/common/UserAvatar'
import { useCurrentUser } from '@/features/auth/use-auth'
import { formatLongDay, pluralize } from '@/lib/format'
import { ROLES, ROLE_LABEL } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'
import { AccountStatusBadge } from './AccountStatusBadge'
import { ConfirmWithPasswordDialog } from './ConfirmWithPasswordDialog'
import { WorkItemList } from './WorkItemList'

type PendingChange = { kind: 'role'; role: Role } | { kind: 'status'; activate: boolean }

/** A user's projects and assigned tasks, with role and account controls for the IT Manager. */
export function UserDetailPage() {
  const { userId = '' } = useParams()
  const me = useCurrentUser()
  const queryClient = useQueryClient()
  const [pending, setPending] = useState<PendingChange | null>(null)
  const detail = useQuery({ queryKey: queryKeys.management.user(userId), queryFn: () => managementApi.user(userId) })

  if (detail.isPending) return <Skeleton className="h-80 rounded-xl" />
  if (detail.isError) return <ErrorState error={detail.error} onRetry={() => detail.refetch()} />
  const { user, ownedProjectCount, overdueTaskCount, projects, tasks } = detail.data
  const isSelf = user.id === me.id
  const active = user.status === 'ACTIVE'

  const apply = async (password: string) => {
    if (!pending) return
    if (pending.kind === 'role') {
      await managementApi.changeRole(user.id, pending.role, password)
      toast.success(`${user.name} is now ${ROLE_LABEL[pending.role]}.`)
    } else {
      await managementApi.changeStatus(user.id, pending.activate ? 'ACTIVE' : 'DEACTIVATED', password)
      toast.success(pending.activate ? `${user.name}'s account was reactivated.` : `${user.name}'s account was deactivated.`)
    }
    void queryClient.invalidateQueries({ queryKey: queryKeys.management.all })
  }

  return (
    <div className="grid gap-6">
      <div>
        <Link to="/management/users" className="mb-2 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
          <ChevronLeft className="size-4" /> Users
        </Link>
        <div className="flex flex-wrap items-center gap-4 rounded-xl border bg-card p-5 shadow-xs">
          <UserAvatar name={user.name} id={user.id} size="lg" />
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-xl font-semibold">{user.name}</h1>
            <p className="truncate text-sm text-muted-foreground">
              {user.email}
              {user.jobTitle ? ` · ${user.jobTitle}` : ''}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
              <span className="inline-flex items-center gap-1 rounded-full bg-accent px-2 py-0.5 font-medium text-accent-foreground">
                <ShieldCheck className="size-3.5" /> {ROLE_LABEL[user.role]}
              </span>
              <AccountStatusBadge status={user.status} />
              <span>Joined {formatLongDay(user.createdAt)}</span>
            </div>
          </div>
          {isSelf ? (
            <p className="text-sm text-muted-foreground">You can't change your own role or status.</p>
          ) : (
            <div className="flex flex-wrap items-center gap-2">
              <Select value={user.role} onValueChange={(role) => role !== user.role && setPending({ kind: 'role', role: role as Role })}>
                <SelectTrigger className="w-44" aria-label="Change role">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ROLES.map((role) => (
                    <SelectItem key={role} value={role}>
                      {ROLE_LABEL[role]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {active ? (
                <Button variant="destructive" onClick={() => setPending({ kind: 'status', activate: false })}>
                  <UserX /> Deactivate
                </Button>
              ) : (
                <Button variant="outline" onClick={() => setPending({ kind: 'status', activate: true })}>
                  <UserCheck /> Reactivate
                </Button>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <StatTile label="Projects" value={user.projectCount} />
        <StatTile label="Projects owned" value={ownedProjectCount} />
        <StatTile label="Active tasks" value={user.activeTaskCount} />
        <StatTile label="Overdue" value={overdueTaskCount} tone={overdueTaskCount > 0 ? 'danger' : 'default'} />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <section className="rounded-xl border bg-card p-5 shadow-xs">
          <h2 className="font-semibold">Projects</h2>
          {projects.length === 0 ? (
            <p className="mt-2 text-sm text-muted-foreground">Not a member of any project.</p>
          ) : (
            <ul className="mt-2 divide-y">
              {projects.map((project) => (
                <li key={project.projectId} className="flex items-center justify-between gap-2 py-2">
                  <Link to={`/projects/${project.projectId}/overview`} className="truncate text-sm font-medium hover:underline">
                    {project.projectName}
                  </Link>
                  <span className="shrink-0 text-xs text-muted-foreground">{project.role === 'OWNER' ? 'Owner' : 'Member'}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
        <section className="rounded-xl border bg-card p-5 shadow-xs lg:col-span-2">
          <h2 className="font-semibold">Assigned tasks</h2>
          <WorkItemList items={tasks} empty="No tasks assigned." />
        </section>
      </div>

      <ConfirmWithPasswordDialog
        open={pending !== null}
        onOpenChange={(open) => !open && setPending(null)}
        destructive={pending?.kind === 'status' && !pending.activate}
        title={
          pending?.kind === 'role'
            ? `Change ${user.name}'s role?`
            : pending?.kind === 'status' && pending.activate
              ? `Reactivate ${user.name}?`
              : `Deactivate ${user.name}?`
        }
        confirmLabel={pending?.kind === 'role' ? 'Change role' : pending?.kind === 'status' && pending.activate ? 'Reactivate' : 'Deactivate'}
        description={
          pending?.kind === 'role' ? (
            <>
              {user.name} will become <strong>{ROLE_LABEL[pending.role]}</strong>. The change applies immediately.
            </>
          ) : pending?.kind === 'status' && pending.activate ? (
            <>{user.name} will be able to sign in again.</>
          ) : (
            <div className="grid gap-2">
              <p>{user.name} will be signed out and can no longer sign in. Their tasks and history are kept.</p>
              {ownedProjectCount > 0 ? (
                <p className="rounded-md bg-amber-50 px-3 py-2 text-amber-900">
                  They own {pluralize(ownedProjectCount, 'project')}. Nobody will be able to manage{' '}
                  {ownedProjectCount === 1 ? 'it' : 'them'} until the account is reactivated.
                </p>
              ) : null}
            </div>
          )
        }
        onConfirm={apply}
      />
    </div>
  )
}
