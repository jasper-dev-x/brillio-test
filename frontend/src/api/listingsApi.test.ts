import { jsonResponse, makeResponse, nonJsonResponse } from '../test/fixtures'
import { ApiRequestError, buildQueryString, fetchCities, fetchListings } from './listingsApi'

const fetchMock = jest.fn() as jest.MockedFunction<typeof fetch>

beforeEach(() => {
  fetchMock.mockReset()
  global.fetch = fetchMock as unknown as typeof fetch
})

describe('buildQueryString', () => {
  it('drops undefined values but keeps zero', () => {
    expect(buildQueryString({ page: 1, pageSize: 5, minBedrooms: 0, city: undefined })).toBe(
      'page=1&pageSize=5&minBedrooms=0',
    )
  })

  it('URL-encodes text', () => {
    expect(buildQueryString({ page: 1, pageSize: 5, keyword: 'pool & spa' })).toBe('page=1&pageSize=5&keyword=pool+%26+spa')
  })
})

describe('fetchListings', () => {
  it('calls the versioned endpoint and returns the parsed body', async () => {
    const body = makeResponse()
    fetchMock.mockResolvedValue(jsonResponse(body))

    await expect(fetchListings({ page: 1, pageSize: 5 })).resolves.toEqual(body)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/listings?page=1&pageSize=5', expect.anything())
  })

  it('maps an RFC 9457 problem response to ApiRequestError with field errors', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        {
          type: 'urn:problem-type:validation-error',
          title: 'Invalid request parameters',
          status: 400,
          detail: 'pageSize must be 1 or greater',
          errors: [{ field: 'pageSize', message: 'pageSize must be 1 or greater' }],
        },
        400,
      ),
    )

    const error = await fetchListings({ page: 1, pageSize: 0 }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiRequestError)
    expect((error as ApiRequestError).status).toBe(400)
    expect((error as ApiRequestError).message).toBe('pageSize must be 1 or greater')
    expect((error as ApiRequestError).fieldErrors).toEqual([{ field: 'pageSize', message: 'pageSize must be 1 or greater' }])
  })

  it('falls back to the title when a problem has no detail', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ status: 404, title: 'Not Found' }, 404))
    await expect(fetchListings({ page: 1, pageSize: 5 })).rejects.toThrow('Not Found')
  })

  it('explains when the backend is unreachable', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))
    await expect(fetchListings({ page: 1, pageSize: 5 })).rejects.toThrow(/Is the backend running/)
  })

  it('handles a non-JSON error body', async () => {
    fetchMock.mockResolvedValue(nonJsonResponse(502))
    await expect(fetchListings({ page: 1, pageSize: 5 })).rejects.toThrow('Request failed with status 502')
  })

  it('re-throws aborts untouched so cancelled requests are not shown as errors', async () => {
    const abort = new Error('aborted')
    abort.name = 'AbortError'
    fetchMock.mockRejectedValue(abort)
    await expect(fetchListings({ page: 1, pageSize: 5 })).rejects.toBe(abort)
  })
})

describe('fetchCities', () => {
  it('returns the city list', async () => {
    fetchMock.mockResolvedValue(jsonResponse(['Reston', 'Vienna']))
    await expect(fetchCities()).resolves.toEqual(['Reston', 'Vienna'])
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/cities', expect.anything())
  })
})
