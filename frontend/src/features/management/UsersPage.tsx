import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { UserSearch } from 'lucide-react'
import { managementApi } from '@/api/endpoints'
import type { Role, UserStatus } from '@/api/types'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { FilterSelect } from '@/components/common/FilterSelect'
import { PageHeader } from '@/components/common/PageHeader'
import { Pagination } from '@/components/common/Pagination'
import { EmptyState, ErrorState, ListSkeleton } from '@/components/common/states'
import { UserAvatar } from '@/components/common/UserAvatar'
import { ROLES, ROLE_LABEL } from '@/lib/labels'
import { queryKeys } from '@/lib/query-keys'
import { useDebouncedValue } from '@/lib/use-debounced-value'
import { AccountStatusBadge } from './AccountStatusBadge'

/** Users page (claude.md §31): search, filter by role, open details. */
export function UsersPage() {
  const navigate = useNavigate()
  const [text, setText] = useState('')
  const [role, setRole] = useState<Role | undefined>()
  const [status, setStatus] = useState<UserStatus | undefined>()
  const [page, setPage] = useState(0)
  const q = useDebouncedValue(text.trim(), 300)
  const filters = { q: q || undefined, role, status, page }
  const users = useQuery({
    queryKey: queryKeys.management.users(filters),
    queryFn: () => managementApi.users(filters),
    placeholderData: keepPreviousData,
  })

  return (
    <div>
      <PageHeader title="Users" description="Everyone with a TaskFlow account." />
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="grid min-w-52 flex-1 gap-1 sm:max-w-xs">
          <Label htmlFor="user-search" className="text-xs text-muted-foreground">
            Search
          </Label>
          <Input
            id="user-search"
            placeholder="Name or email…"
            value={text}
            onChange={(event) => {
              setText(event.target.value)
              setPage(0)
            }}
            className="bg-card"
          />
        </div>
        <FilterSelect
          id="user-role"
          label="Role"
          value={role}
          options={ROLES.map((value) => ({ value, label: ROLE_LABEL[value] }))}
          anyLabel="All roles"
          onChange={(value) => {
            setRole(value as Role | undefined)
            setPage(0)
          }}
        />
        <FilterSelect
          id="user-status"
          label="Status"
          value={status}
          options={[
            { value: 'ACTIVE', label: 'Active' },
            { value: 'DEACTIVATED', label: 'Deactivated' },
          ]}
          anyLabel="All"
          onChange={(value) => {
            setStatus(value as UserStatus | undefined)
            setPage(0)
          }}
        />
      </div>

      {users.isPending ? (
        <ListSkeleton rows={6} />
      ) : users.isError ? (
        <ErrorState error={users.error} onRetry={() => users.refetch()} />
      ) : users.data.items.length === 0 ? (
        <EmptyState icon={<UserSearch />} title="No users found" description="Try another name, email or filter." />
      ) : (
        <div className={users.isPlaceholderData ? 'opacity-70 transition-opacity' : undefined}>
          <div className="overflow-x-auto rounded-xl border bg-card shadow-xs">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Projects</TableHead>
                  <TableHead className="text-right">Active tasks</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.data.items.map((user) => (
                  <TableRow
                    key={user.id}
                    className="cursor-pointer"
                    tabIndex={0}
                    onClick={() => navigate(`/management/users/${user.id}`)}
                    onKeyDown={(event) => event.key === 'Enter' && navigate(`/management/users/${user.id}`)}
                  >
                    <TableCell>
                      <span className="flex items-center gap-2 font-medium">
                        <UserAvatar name={user.name} id={user.id} size="sm" />
                        {user.name}
                      </span>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{user.email}</TableCell>
                    <TableCell>{ROLE_LABEL[user.role]}</TableCell>
                    <TableCell>
                      <AccountStatusBadge status={user.status} />
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{user.projectCount}</TableCell>
                    <TableCell className="text-right tabular-nums">{user.activeTaskCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <Pagination page={users.data.page} totalPages={users.data.totalPages} totalItems={users.data.totalItems} onPageChange={setPage} />
        </div>
      )}
    </div>
  )
}
