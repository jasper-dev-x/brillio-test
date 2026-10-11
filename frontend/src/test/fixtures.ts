import type { ListingItem, SearchResponse } from '../api/types'

export function makeItem(overrides: Partial<ListingItem> = {}): ListingItem {
  return {
    id: 'A1',
    source: 'MLS_A',
    address: '123 Main St, Apt 4B',
    city: 'Springfield',
    state: 'VA',
    zip: '22150',
    price: 450000,
    bedrooms: 2,
    bathrooms: 1.5,
    sqft: 980,
    latitude: 38.79,
    longitude: -77.19,
    listedDate: '2026-08-29',
    status: 'active',
    description: 'Bright top-floor condo',
    score: 87.5,
    scoreBreakdown: { budget: 0.9, recency: 0.8 },
    possibleDuplicates: [],
    ...overrides,
  }
}

export function makeResponse(overrides: Partial<SearchResponse> = {}): SearchResponse {
  return {
    items: [makeItem()],
    page: 1,
    pageSize: 5,
    totalItems: 1,
    totalPages: 1,
    notices: [],
    ...overrides,
  }
}

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}
