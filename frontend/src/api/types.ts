/** Mirrors the Spring Boot API contract (dto.SearchResponse / RFC 9457 ProblemDetail). */

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

/** RFC 9457 problem details, plus the API's `errors` extension for validation failures. */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  errors?: FieldError[]
}

/** Query parameters accepted by GET /api/v1/listings. Omitted = not filtered. */
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
