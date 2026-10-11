/** Mirrors the Spring Boot API contract (com.listings.search.SearchResult / web.ApiError). */

export type ListingStatus = 'active' | 'pending' | 'sold'

export interface DuplicateRef {
  id: string
  source: string
  address: string
  price: number
}

export interface ScoreBreakdown {
  /** null when no targetBudget was supplied */
  budget: number | null
  recency: number
}

export interface ListingItem {
  id: string
  source: string
  address: string
  city: string
  state: string
  zip: string
  price: number
  bedrooms: number
  bathrooms: number
  sqft: number
  latitude: number
  longitude: number
  listedDate: string
  status: ListingStatus
  description: string
  score: number
  scoreBreakdown: ScoreBreakdown
  possibleDuplicates: DuplicateRef[]
}

export interface SearchResponse {
  items: ListingItem[]
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
  notices: string[]
}

export interface FieldError {
  field: string
  message: string
}

export interface ApiErrorBody {
  status: number
  error: string
  message: string
  details: FieldError[]
}

/** Query parameters accepted by GET /api/listings. Omitted = not filtered. */
export interface SearchParams {
  minPrice?: number
  maxPrice?: number
  minBedrooms?: number
  city?: string
  keyword?: string
  targetBudget?: number
  status?: ListingStatus
  page: number
  pageSize: number
}
