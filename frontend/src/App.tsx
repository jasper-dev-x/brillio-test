import { useEffect } from 'react'

import { useAppDispatch } from './app/hooks'
import { ResultsPanel } from './features/search/ResultsPanel'
import { SearchForm } from './features/search/SearchForm'
import { loadCities, submitSearch } from './features/search/searchSlice'

export default function App() {
  const dispatch = useAppDispatch()

  useEffect(() => {
    void dispatch(loadCities())
    void dispatch(submitSearch())
  }, [dispatch])

  return (
    <div className="min-h-screen bg-base-200">
      <header className="navbar border-b border-base-300 bg-base-100 px-6">
        <div className="flex-1">
          <h1 className="text-lg font-semibold tracking-tight">Listing Search</h1>
        </div>
        <span className="text-xs text-base-content/60">MLS_A + MLS_B feeds</span>
      </header>

      <main className="mx-auto grid max-w-7xl gap-6 p-4 md:p-6 lg:grid-cols-[18rem_1fr]">
        <aside className="card h-fit bg-base-100 shadow-sm lg:sticky lg:top-6">
          <div className="card-body p-5">
            <h2 className="card-title text-base">Filters</h2>
            <SearchForm />
          </div>
        </aside>

        <div className="card bg-base-100 shadow-sm">
          <div className="card-body p-5">
            <ResultsPanel />
          </div>
        </div>
      </main>
    </div>
  )
}
