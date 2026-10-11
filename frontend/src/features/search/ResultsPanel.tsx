import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { Pagination } from './Pagination'
import { ResultsList } from './ResultsList'
import { goToPage, submitSearch } from './searchSlice'

/** Owns the loading / error / empty / results states for the right-hand panel. */
export function ResultsPanel() {
  const dispatch = useAppDispatch()
  const { status, result, error } = useAppSelector((s) => s.search)
  const loading = status === 'loading'

  if (status === 'failed') {
    return (
      <div role="alert" className="alert alert-error alert-soft">
        <div>
          <h3 className="font-semibold">Search failed</h3>
          <p className="text-sm">{error}</p>
        </div>
        <button type="button" className="btn btn-sm" onClick={() => void dispatch(submitSearch())}>
          Retry
        </button>
      </div>
    )
  }

  if (!result) {
    // First load: nothing to show yet.
    return (
      <div className="flex flex-col gap-3" aria-busy="true" aria-label="Loading results">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="skeleton h-20 w-full" />
        ))}
      </div>
    )
  }

  const { items, page, pageSize, totalItems, totalPages, notices } = result
  const from = totalItems === 0 ? 0 : (page - 1) * pageSize + 1
  const to = Math.min(page * pageSize, totalItems)

  return (
    <section aria-busy={loading} className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm text-base-content/70" aria-live="polite">
          {totalItems === 0 ? 'No results' : `Showing ${from}–${to} of ${totalItems} listings, ranked by relevance`}
        </p>
        {loading && <span className="loading loading-dots loading-sm" aria-label="Loading" />}
      </div>

      {notices.map((n) => (
        <div key={n} role="status" className="alert alert-info alert-soft text-sm">
          {n}
        </div>
      ))}

      {totalItems === 0 ? (
        <div className="rounded-box border border-dashed border-base-300 p-10 text-center">
          <h3 className="font-semibold">No listings match your search</h3>
          <p className="mt-1 text-sm text-base-content/60">Try widening the price range or removing a filter.</p>
        </div>
      ) : (
        <div className={`transition-opacity ${loading ? 'opacity-50' : ''}`}>
          <ResultsList items={items} startRank={from} />
        </div>
      )}

      <div className="flex justify-center">
        <Pagination
          page={page}
          totalPages={totalPages}
          disabled={loading}
          onChange={(p) => void dispatch(goToPage(p))}
        />
      </div>
    </section>
  )
}
