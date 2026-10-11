import { expect, test } from '@playwright/test'

import { SearchPage } from './pages/SearchPage'

/**
 * End-to-end against the real Spring Boot API and sample_listings.json (12 listings, 6 cities).
 * Assertions avoid exact scores because recency depends on today's date; they check ordering,
 * counts and behaviour instead.
 */
test.describe('Listing search', () => {
  let search: SearchPage

  test.beforeEach(async ({ page }) => {
    search = new SearchPage(page)
    await search.goto()
  })

  test('loads the first page of all listings with the required columns', async () => {
    await expect(search.summary).toHaveText('Showing 1–5 of 12 listings, ranked by relevance')
    await expect(search.rows).toHaveCount(5)

    const headers = await search.results.locator('thead th').allInnerTexts()
    expect(headers.map((h) => h.trim())).toEqual(expect.arrayContaining(['Address', 'Price', 'Beds', 'Score']))

    // Without a target budget the ranking is pure recency: newest listing (55 Elm Ct, 2026-09-04) first.
    await expect(search.row(0)).toContainText('55 Elm Ct')
    await expect(search.row(0)).toContainText('$470,000')
  })

  test('filters by city, bedrooms and keyword together', async () => {
    await search.fill({ City: 'springfield', 'Min bedrooms': '3', Keyword: 'kitchen' })
    await search.search()

    await expect(search.summary).toHaveText('Showing 1–2 of 2 listings, ranked by relevance')
    expect((await search.addresses()).sort()).toEqual(['456 Oak Ave', '456 Oak Avenue'])
  })

  test('filters by price range (inclusive)', async () => {
    await search.fill({ 'Min price': '399500', 'Max price': '415000' })
    await search.search()

    await expect(search.rows).toHaveCount(2)
    expect((await search.addresses()).sort()).toEqual(['300 Cedar Blvd', '789 Pine Rd'])
  })

  test('ranks the listing closest to the target budget first', async () => {
    await search.fill({ City: 'Springfield', 'Target budget': '450000' })
    await search.search()

    // 123 Main St lists at exactly 450k; its MLS_B twin is 452k and listed earlier.
    await expect(search.row(0)).toContainText('123 Main St, Apt 4B')
    await expect(search.row(1)).toContainText('123 Main Street, Unit 4B')

    const scores = (await search.results.locator('[aria-label^="Relevance score"]').allInnerTexts()).map(Number)
    expect(scores).toEqual([...scores].sort((a, b) => b - a))
  })

  test('paginates through every result without repeats', async () => {
    const seen: string[] = [...(await search.listingIds())]

    await search.goToPage('Next page')
    await expect(search.summary).toHaveText('Showing 6–10 of 12 listings, ranked by relevance')
    seen.push(...(await search.listingIds()))

    await search.goToPage('Page 3')
    await expect(search.summary).toHaveText('Showing 11–12 of 12 listings, ranked by relevance')
    await expect(search.page.getByRole('button', { name: 'Next page' })).toBeDisabled()
    seen.push(...(await search.listingIds()))

    expect(seen).toHaveLength(12)
    expect(new Set(seen).size).toBe(12) // stable ordering: no listing appears on two pages

    await search.goToPage('Previous page')
    await expect(search.page.getByRole('button', { name: 'Page 2' })).toHaveAttribute('aria-current', 'page')
  })

  test('changing page size re-paginates from page 1', async () => {
    await search.goToPage('Next page')
    await search.page.getByLabel('Results per page').selectOption('10')
    await search.search()

    await expect(search.summary).toHaveText('Showing 1–10 of 12 listings, ranked by relevance')
    await expect(search.rows).toHaveCount(10)
  })

  test('blocks an invalid price range before calling the API', async ({ page }) => {
    let apiCalls = 0
    page.on('request', (r) => {
      if (r.url().includes('/api/v1/listings')) apiCalls++
    })

    await search.fill({ 'Min price': '600000', 'Max price': '400000' })
    await search.searchButton.click()

    await expect(page.getByText('Min price cannot be greater than max price')).toBeVisible()
    await expect(search.field('Min price')).toHaveAttribute('aria-invalid', 'true')
    expect(apiCalls).toBe(0)
  })

  test('explains an unknown city instead of silently showing nothing', async ({ page }) => {
    await search.fill({ City: 'Gotham' })
    await search.search()

    await expect(page.getByText('No listings match your search')).toBeVisible()
    await expect(page.getByRole('status')).toContainText("No listings found in city 'Gotham'")
    await expect(page.getByRole('status')).toContainText('Springfield')
  })

  test('shows the empty state when valid filters match nothing', async ({ page }) => {
    await search.fill({ 'Min price': '5000000' })
    await search.search()

    await expect(page.getByText('No listings match your search')).toBeVisible()
    await expect(page.getByRole('status')).toHaveCount(0)
  })

  test('flags cross-feed duplicates', async () => {
    await search.fill({ Keyword: 'top floor condo' })
    await search.search()

    await expect(search.rows).toHaveCount(2)
    await expect(search.results.getByTestId('duplicate-badge')).toHaveCount(2)
  })

  test('Reset restores the default search', async () => {
    await search.fill({ City: 'Reston' })
    await search.search()
    await expect(search.rows).toHaveCount(2)

    const response = search.page.waitForResponse((r) => r.url().includes('/api/v1/listings'))
    await search.resetButton.click()
    await response

    await expect(search.field('City')).toHaveValue('')
    await expect(search.summary).toHaveText('Showing 1–5 of 12 listings, ranked by relevance')
  })
})

test.describe('API contract (direct HTTP)', () => {
  test('rejects invalid parameters with an RFC 9457 problem listing every bad field', async ({ request }) => {
    const res = await request.get('/api/v1/listings?minPrice=600000&maxPrice=400000&pageSize=0')

    expect(res.status()).toBe(400)
    expect(res.headers()['content-type']).toContain('application/problem+json')
    const body = await res.json()
    expect(body.title).toBe('Invalid request parameters')
    expect(body.errors.map((e: { field: string }) => e.field).sort()).toEqual(['minPrice', 'pageSize'])
  })

  test('rejects a page past the end', async ({ request }) => {
    const res = await request.get('/api/v1/listings?page=99')
    expect(res.status()).toBe(400)
    expect((await res.json()).errors[0].field).toBe('page')
  })

  test('rejects non-numeric values with a readable message', async ({ request }) => {
    const res = await request.get('/api/v1/listings?minPrice=cheap')
    expect(res.status()).toBe(400)
    expect((await res.json()).errors[0].message).toBe("minPrice must be a number (got 'cheap')")
  })
})
