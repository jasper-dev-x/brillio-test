import type { FieldError, ProblemDetail, SearchParams, SearchResponse } from './types'

export const API_BASE = '/api/v1'

/** Any non-2xx response or network failure, normalized for the UI. */
export class ApiRequestError extends Error {
  readonly status: number
  readonly fieldErrors: FieldError[]

  constructor(message: string, status: number, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export function buildQueryString(params: SearchParams): string {
  const qs = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue
    qs.set(key, String(value))
  }
  return qs.toString()
}

async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  let res: Response
  try {
    res = await fetch(url, { signal, headers: { Accept: 'application/json, application/problem+json' } })
  } catch (e) {
    if (e instanceof Error && e.name === 'AbortError') throw e
    throw new ApiRequestError('Could not reach the listings service. Is the backend running?', 0)
  }

  if (!res.ok) {
    const problem = (await res.json().catch(() => null)) as Partial<ProblemDetail> | null
    throw new ApiRequestError(
      problem?.detail ?? problem?.title ?? `Request failed with status ${res.status}`,
      res.status,
      problem?.errors ?? [],
    )
  }
  return (await res.json()) as T
}

export function fetchListings(params: SearchParams, signal?: AbortSignal): Promise<SearchResponse> {
  return getJson<SearchResponse>(`${API_BASE}/listings?${buildQueryString(params)}`, signal)
}

export function fetchCities(signal?: AbortSignal): Promise<string[]> {
  return getJson<string[]>(`${API_BASE}/cities`, signal)
}
