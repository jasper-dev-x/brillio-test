import type { ApiErrorBody, FieldError, SearchParams, SearchResponse } from './types'

/** Error thrown for any non-2xx response or network failure, normalized for the UI. */
export class ApiRequestError extends Error {
  readonly status: number
  readonly details: FieldError[]

  constructor(message: string, status: number, details: FieldError[] = []) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.details = details
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
    res = await fetch(url, { signal, headers: { Accept: 'application/json' } })
  } catch (e) {
    if ((e as Error).name === 'AbortError') throw e
    throw new ApiRequestError('Could not reach the listings service. Is the backend running?', 0)
  }

  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as Partial<ApiErrorBody> | null
    throw new ApiRequestError(
      body?.message ?? `Request failed with status ${res.status}`,
      res.status,
      body?.details ?? [],
    )
  }
  return (await res.json()) as T
}

export function fetchListings(params: SearchParams, signal?: AbortSignal): Promise<SearchResponse> {
  return getJson<SearchResponse>(`/api/listings?${buildQueryString(params)}`, signal)
}

export function fetchCities(signal?: AbortSignal): Promise<string[]> {
  return getJson<string[]>('/api/cities', signal)
}
