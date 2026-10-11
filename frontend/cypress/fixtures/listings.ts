import type { ListingItem, SearchResponse } from '../../src/api/types'

export function item(overrides: Partial<ListingItem> = {}): ListingItem {
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
    description: 'Bright top-floor condo near shops and transit.',
    score: 87.5,
    scoreBreakdown: { budget: 0.9, recency: 0.8 },
    possibleDuplicates: [],
    ...overrides,
  }
}

export function page(overrides: Partial<SearchResponse> = {}): SearchResponse {
  return { items: [item()], page: 1, pageSize: 5, totalItems: 1, totalPages: 1, notices: [], ...overrides }
}
