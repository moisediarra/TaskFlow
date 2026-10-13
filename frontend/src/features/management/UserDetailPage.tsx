import { useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router'
import { ChevronLeft, KeyRound, ShieldCheck, Trash2, UserCheck, UserPen, UserX } from 'lucide-react'
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
import { ROLES, ROLE_HINT, ROLE_LABEL } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'
import { AccountStatusBadge } from './AccountStatusBadge'
import { ConfirmWithPasswordDialog } from './ConfirmWithPasswordDialog'
import { ResetPasswordDialog } from './ResetPasswordDialog'
import { UserFormDialog } from './UserFormDialog'
import { WorkItemList } from './WorkItemList'

type PendingChange = { kind: 'role'; role: Role } | { kind: 'status'; activate: boolean } | { kind: 'delete' }

/** A user's projects and assigned tasks, with the IT Manager's account administration (claude.md §31). */
export function UserDetailPage() {
  const { userId = '' } = useParams()
  const me = useCurrentUser()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [pending, setPending] = useState<PendingChange | null>(null)
  const [editing, setEditing] = useState(false)
  const [resetting, setResetting] = useState(false)
  const detail = useQuery({ queryKey: queryKeys.management.user(userId), queryFn: () => managementApi.user(userId) })

  if (detail.isPending) return <Skeleton className="h-80 rounded-xl" />
  if (detail.isError) return <ErrorState error={detail.error} onRetry={() => detail.refetch()} />
  const { user, ownedProjectCount, overdueTaskCount, projects, tasks } = detail.data
  const isSelf = user.id === me.id
  const active = user.status === 'ACTIVE'

  const apply = async (password: string) => {
    if (!pending) return
    if (pending.kind === 'delete') {
      await managementApi.deleteUser(user.id, password)
      toast.success(`${user.name}'s account was deleted.`)
      // Leave the page first, so nothing refetches the account that no longer exists.
      await navigate('/management/users', { replace: true })
      queryClient.removeQueries({ queryKey: queryKeys.management.user(user.id) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.management.all })
      return
    }
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
          <Button variant="outline" onClick={() => setEditing(true)}>
            <UserPen /> Edit details
          </Button>
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

      <section className="rounded-xl border bg-card shadow-xs" aria-labelledby="account-heading">
        <div className="px-5 pt-5">
          <h2 id="account-heading" className="font-semibold">
            Account
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">Each change asks for your password and is recorded in the activity log.</p>
        </div>
        {isSelf ? (
          <p className="px-5 pt-3 pb-5 text-sm text-muted-foreground">
            This is your account. Change your password from your{' '}
            <Link to="/profile" className="font-medium text-foreground underline-offset-4 hover:underline">
              profile
            </Link>
            . Another IT Manager can change your role or status.
          </p>
        ) : (
          <ul className="mt-2 divide-y">
            <AccountRow title="Role" description={ROLE_HINT[user.role]}>
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
            </AccountRow>
            <AccountRow title="Password" description={`Set a new password and sign ${user.name} out everywhere.`}>
              <Button variant="outline" onClick={() => setResetting(true)}>
                <KeyRound /> Reset password
              </Button>
            </AccountRow>
            <AccountRow
              title={active ? 'Deactivate' : 'Reactivate'}
              description={
                active
                  ? "They can no longer sign in until reactivated. Their tasks and history are kept."
                  : 'This account is deactivated and cannot sign in.'
              }
            >
              {active ? (
                <Button variant="outline" onClick={() => setPending({ kind: 'status', activate: false })}>
                  <UserX /> Deactivate
                </Button>
              ) : (
                <Button variant="outline" onClick={() => setPending({ kind: 'status', activate: true })}>
                  <UserCheck /> Reactivate
                </Button>
              )}
            </AccountRow>
            <AccountRow
              title="Delete account"
              description={
                ownedProjectCount > 0
                  ? `${user.name} owns ${pluralize(ownedProjectCount, 'project')}. Delete ${ownedProjectCount === 1 ? 'it' : 'them'} first, or deactivate the account instead.`
                  : 'Permanently removes the account. Their tasks stay in their projects, unassigned, and the activity history keeps their name.'
              }
            >
              <Button variant="destructive" disabled={ownedProjectCount > 0} onClick={() => setPending({ kind: 'delete' })}>
                <Trash2 /> Delete account
              </Button>
            </AccountRow>
          </ul>
        )}
      </section>

      <UserFormDialog open={editing} onOpenChange={setEditing} user={user} />
      <ResetPasswordDialog open={resetting} onOpenChange={setResetting} user={user} />
      <ConfirmWithPasswordDialog
        open={pending !== null}
        onOpenChange={(open) => !open && setPending(null)}
        destructive={pending?.kind === 'delete' || (pending?.kind === 'status' && !pending.activate)}
        title={confirmTitle(pending, user.name)}
        confirmLabel={confirmLabel(pending)}
        description={
          pending?.kind === 'role' ? (
            <>
              {user.name} will become <strong>{ROLE_LABEL[pending.role]}</strong>. The change applies immediately.
            </>
          ) : pending?.kind === 'status' && pending.activate ? (
            <>{user.name} will be able to sign in again.</>
          ) : pending?.kind === 'delete' ? (
            <div className="grid gap-2">
              <p>
                This can't be undone. {user.name} will be signed out
                {projects.length > 0 ? ` and removed from ${pluralize(projects.length, 'project')}` : ''}.
              </p>
              <p>Their tasks stay in their projects, unassigned, and the activity history keeps their name.</p>
              <p>To keep the account for later, deactivate it instead.</p>
            </div>
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

function confirmTitle(pending: PendingChange | null, name: string) {
  if (pending?.kind === 'role') return `Change ${name}'s role?`
  if (pending?.kind === 'delete') return `Delete ${name}'s account?`
  if (pending?.kind === 'status' && pending.activate) return `Reactivate ${name}?`
  return `Deactivate ${name}?`
}

function confirmLabel(pending: PendingChange | null) {
  if (pending?.kind === 'role') return 'Change role'
  if (pending?.kind === 'delete') return 'Delete account'
  if (pending?.kind === 'status' && pending.activate) return 'Reactivate'
  return 'Deactivate'
}

/** One administrative action: what it does on the left, its control on the right (stacked on phones). */
function AccountRow({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <li className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0">
        <p className="text-sm font-medium">{title}</p>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      <div className="shrink-0">{children}</div>
    </li>
  )
}
