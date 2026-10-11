import { pageWindow } from '../../lib/format'

interface Props {
  page: number
  totalPages: number
  disabled?: boolean
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, disabled = false, onChange }: Props) {
  if (totalPages <= 1) return null

  return (
    <nav aria-label="Pagination" className="join">
      <button
        type="button"
        className="join-item btn btn-sm"
        disabled={disabled || page <= 1}
        onClick={() => onChange(page - 1)}
        aria-label="Previous page"
      >
        «
      </button>
      {pageWindow(page, totalPages).map((p, i) =>
        p === null ? (
          <button key={`gap-${i}`} type="button" className="join-item btn btn-sm btn-disabled" tabIndex={-1}>
            …
          </button>
        ) : (
          <button
            key={p}
            type="button"
            className={`join-item btn btn-sm ${p === page ? 'btn-active btn-primary' : ''}`}
            aria-current={p === page ? 'page' : undefined}
            aria-label={`Page ${p}`}
            disabled={disabled}
            onClick={() => p !== page && onChange(p)}
          >
            {p}
          </button>
        ),
      )}
      <button
        type="button"
        className="join-item btn btn-sm"
        disabled={disabled || page >= totalPages}
        onClick={() => onChange(page + 1)}
        aria-label="Next page"
      >
        »
      </button>
    </nav>
  )
}
