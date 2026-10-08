import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface PaginationProps {
  page: number
  totalPages: number
  totalItems: number
  onPageChange: (page: number) => void
}

/** Page-numbered navigation for large tables (claude.md §38). Pages are 0-based. */
export function Pagination({ page, totalPages, totalItems, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return <p className="mt-3 text-sm text-muted-foreground">{totalItems} {totalItems === 1 ? 'result' : 'results'}</p>
  }
  return (
    <div className="mt-3 flex items-center justify-between gap-2 text-sm">
      <p className="text-muted-foreground">
        Page {page + 1} of {totalPages} · {totalItems} results
      </p>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft /> Previous
        </Button>
        <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>
          Next <ChevronRight />
        </Button>
      </div>
    </div>
  )
}
