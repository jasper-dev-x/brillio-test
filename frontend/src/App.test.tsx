import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from './App'
import { makeStore } from './app/store'
import { jsonResponse, makeItem, makeResponse } from './test/fixtures'

type Handler = (url: URL) => Response

function mockApi(onListings: Handler) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = new URL(String(input), 'http://localhost')
    if (url.pathname === '/api/cities') return jsonResponse(['Reston', 'Springfield'])
    return onListings(url)
  })
}

function renderApp() {
  const store = makeStore()
  render(
    <Provider store={store}>
      <App />
    </Provider>,
  )
  return store
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('App', () => {
  it('loads and renders ranked results with address, price, beds and score', async () => {
    mockApi(() =>
      jsonResponse(
        makeResponse({
          items: [
            makeItem({ id: 'A1', address: '123 Main St', price: 450000, bedrooms: 2, score: 91.2 }),
            makeItem({ id: 'B7', source: 'MLS_B', address: '456 Oak Ave', price: 525000, bedrooms: 3, score: 70 }),
          ],
          totalItems: 2,
        }),
      ),
    )
    renderApp()

    const table = await screen.findByRole('table', { name: 'Search results' })
    const rows = within(table).getAllByRole('row').slice(1)
    expect(rows).toHaveLength(2)
    expect(rows[0]).toHaveTextContent('123 Main St')
    expect(rows[0]).toHaveTextContent('$450,000')
    expect(rows[0]).toHaveTextContent('91.2')
    expect(rows[1]).toHaveTextContent('456 Oak Ave')
    expect(screen.getByText(/Showing 1–2 of 2 listings/)).toBeInTheDocument()
  })

  it('shows the no-results state and server notices', async () => {
    mockApi(() =>
      jsonResponse(makeResponse({ items: [], totalItems: 0, totalPages: 0, notices: ["No listings found in city 'Gotham'."] })),
    )
    renderApp()
    expect(await screen.findByText('No listings match your search')).toBeInTheDocument()
    expect(screen.getByText(/Gotham/)).toBeInTheDocument()
  })

  it('shows an error state when the API fails', async () => {
    mockApi(() => jsonResponse({ status: 500, error: 'x', message: 'Something went wrong. Please try again.', details: [] }, 500))
    renderApp()
    expect(await screen.findByText('Search failed')).toBeInTheDocument()
    expect(screen.getByText('Something went wrong. Please try again.')).toBeInTheDocument()
  })

  it('shows inline validation and does not call the API for min > max', async () => {
    const spy = mockApi(() => jsonResponse(makeResponse()))
    renderApp()
    await screen.findByRole('table')
    const callsBefore = spy.mock.calls.length

    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Min price'), '500000')
    await user.type(screen.getByLabelText('Max price'), '100')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByText('Min price cannot be greater than max price')).toBeInTheDocument()
    expect(screen.getByLabelText('Min price')).toHaveAttribute('aria-invalid', 'true')
    expect(spy.mock.calls.length).toBe(callsBefore)
  })

  it('submits filters and paginates', async () => {
    const spy = mockApi((url) => {
      const page = Number(url.searchParams.get('page'))
      return jsonResponse(
        makeResponse({
          items: [makeItem({ id: `P${page}`, address: `${page} Page St` })],
          page,
          pageSize: 1,
          totalItems: 3,
          totalPages: 3,
        }),
      )
    })
    renderApp()
    await screen.findByText('1 Page St')

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Next page' }))
    expect(await screen.findByText('2 Page St')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Page 2' })).toHaveAttribute('aria-current', 'page')

    await user.click(screen.getByRole('button', { name: 'Page 3' }))
    await screen.findByText('3 Page St')
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled()

    await user.type(screen.getByLabelText('City'), 'Reston')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() => {
      const last = new URL(String(spy.mock.calls.at(-1)![0]), 'http://localhost')
      expect(last.searchParams.get('city')).toBe('Reston')
      expect(last.searchParams.get('page')).toBe('1')
    })
  })

  it('flags possible duplicates', async () => {
    mockApi(() =>
      jsonResponse(
        makeResponse({
          items: [makeItem({ possibleDuplicates: [{ id: 'B7', source: 'MLS_B', address: '123 Main Street', price: 452000 }] })],
        }),
      ),
    )
    renderApp()
    expect(await screen.findByTestId('duplicate-badge')).toHaveTextContent('Possible duplicate')
  })
})
