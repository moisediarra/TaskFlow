import { Link } from 'react-router'
import { ShieldAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'

export function ForbiddenPage() {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-3 py-24 text-center">
      <ShieldAlert className="size-10 text-amber-500" aria-hidden />
      <h1 className="text-xl font-semibold">You don't have access to this page</h1>
      <p className="text-sm text-muted-foreground">
        It's reserved for another role or belongs to a project you're not part of. Ask the project owner or your IT Manager
        if you think you should have access.
      </p>
      <Button asChild variant="outline">
        <Link to="/">Back to TaskFlow</Link>
      </Button>
    </div>
  )
}
