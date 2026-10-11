import { afterEach, describe, expect, it, vi } from 'vitest'

import { jsonResponse, makeResponse } from '../test/fixtures'
import { ApiRequestError, buildQueryString, fetchListings } from './listingsApi'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('buildQueryString', () => {
  it('drops undefined values and keeps zero', () => {
    expect(buildQueryString({ page: 1, pageSize: 5, minBedrooms: 0, city: undefined })).toBe(
      'page=1&pageSize=5&minBedrooms=0',
    )
  })

  it('encodes text', () => {
    expect(buildQueryString({ page: 1, pageSize: 5, keyword: 'pool & spa' })).toBe('page=1&pageSize=5&keyword=pool+%26+spa')
  })
})

describe('fetchListings', () => {
  it('returns the parsed body on success', async () => {
    const body = makeResponse()
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(body))
    await expect(fetchListings({ page: 1, pageSize: 5 })).resolves.toEqual(body)
    expect(spy).toHaveBeenCalledWith('/api/listings?page=1&pageSize=5', expect.anything())
  })

  it('maps a 400 body to ApiRequestError with field details', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(
        { status: 400, error: 'Bad Request', message: 'pageSize must be 1 or greater', details: [{ field: 'pageSize', message: 'x' }] },
        400,
      ),
    )
    const err = await fetchListings({ page: 1, pageSize: 0 }).catch((e: unknown) => e)
    expect(err).toBeInstanceOf(ApiRequestError)
    expect((err as ApiRequestError).status).toBe(400)
    expect((err as ApiRequestError).message).toBe('pageSize must be 1 or greater')
    expect((err as ApiRequestError).details[0].field).toBe('pageSize')
  })

  it('gives a helpful message when the backend is unreachable', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))
    await expect(fetchListings({ page: 1, pageSize: 5 })).rejects.toThrow(/Is the backend running/)
  })

  it('handles a non-JSON error body', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('<html>oops</html>', { status: 502 }))
    await expect(fetchListings({ page: 1, pageSize: 5 })).rejects.toThrow('Request failed with status 502')
  })
})
