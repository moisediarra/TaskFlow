import { Link, isRouteErrorResponse, useRouteError } from 'react-router'
import { Compass, TriangleAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-3 py-24 text-center">
      <Compass className="size-10 text-muted-foreground" aria-hidden />
      <h1 className="text-xl font-semibold">Page not found</h1>
      <p className="text-sm text-muted-foreground">The page you're looking for doesn't exist or was moved.</p>
      <Button asChild variant="outline">
        <Link to="/">Back to TaskFlow</Link>
      </Button>
    </div>
  )
}

/** Last-resort boundary for rendering errors; never shows a stack trace. */
export function RouteErrorPage() {
  const error = useRouteError()
  if (isRouteErrorResponse(error) && error.status === 404) return <NotFoundPage />
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-3 px-4 py-24 text-center">
      <TriangleAlert className="size-10 text-amber-500" aria-hidden />
      <h1 className="text-xl font-semibold">Something went wrong</h1>
      <p className="text-sm text-muted-foreground">This page ran into a problem. Reloading usually fixes it.</p>
      <Button onClick={() => window.location.reload()}>Reload</Button>
    </div>
  )
}
