import { makeStore } from '../../app/store'
import { jsonResponse, makeResponse } from '../../test/fixtures'
import reducer, {
  fieldChanged,
  fieldErrorsSet,
  formReset,
  goToPage,
  initialState,
  loadCities,
  runSearch,
  submitSearch,
} from './searchSlice'

const fetchMock = jest.fn() as jest.MockedFunction<typeof fetch>

beforeEach(() => {
  fetchMock.mockReset()
  global.fetch = fetchMock as unknown as typeof fetch
})

const requestedUrls = () => fetchMock.mock.calls.map(([url]) => String(url))

describe('reducers', () => {
  it('updates a field and clears its error', () => {
    const withError = reducer(initialState, fieldErrorsSet({ city: 'bad' }))
    const next = reducer(withError, fieldChanged({ field: 'city', value: 'Reston' }))

    expect(next.form.city).toBe('Reston')
    expect(next.fieldErrors.city).toBeUndefined()
  })

  it('editing either price bound clears both price errors', () => {
    const withErrors = reducer(initialState, fieldErrorsSet({ minPrice: 'x', maxPrice: 'y' }))
    expect(reducer(withErrors, fieldChanged({ field: 'maxPrice', value: '1' })).fieldErrors).toEqual({})
  })

  it('formReset restores the defaults', () => {
    const edited = reducer(initialState, fieldChanged({ field: 'keyword', value: 'pool' }))
    expect(reducer(edited, formReset()).form).toEqual(initialState.form)
  })

  it('ignores a stale response that arrives after a newer request started', () => {
    let state = reducer(initialState, { type: runSearch.pending.type, meta: { requestId: 'old', arg: { page: 1, pageSize: 5 } } })
    state = reducer(state, { type: runSearch.pending.type, meta: { requestId: 'new', arg: { page: 2, pageSize: 5 } } })
    state = reducer(state, {
      type: runSearch.fulfilled.type,
      payload: makeResponse({ page: 1 }),
      meta: { requestId: 'old', arg: { page: 1, pageSize: 5 } },
    })

    expect(state.status).toBe('loading')
    expect(state.result).toBeNull()
  })
})

describe('thunks', () => {
  it('submitSearch does not call the API when the form is invalid', async () => {
    const store = makeStore()
    store.dispatch(fieldChanged({ field: 'minPrice', value: '9' }))
    store.dispatch(fieldChanged({ field: 'maxPrice', value: '1' }))

    await store.dispatch(submitSearch())

    expect(fetchMock).not.toHaveBeenCalled()
    expect(store.getState().search.fieldErrors.minPrice).toMatch(/greater than max/)
  })

  it('submitSearch always starts from page 1 and stores the result', async () => {
    fetchMock.mockResolvedValue(jsonResponse(makeResponse({ totalItems: 1 })))
    const store = makeStore()
    store.dispatch(fieldChanged({ field: 'city', value: 'Reston' }))

    await store.dispatch(submitSearch())

    expect(requestedUrls()).toEqual(['/api/v1/listings?city=Reston&page=1&pageSize=5'])
    expect(store.getState().search.status).toBe('succeeded')
    expect(store.getState().search.result?.totalItems).toBe(1)
  })

  it('goToPage reuses the applied search, not unsubmitted form edits', async () => {
    fetchMock.mockImplementation(async () => jsonResponse(makeResponse()))
    const store = makeStore()
    store.dispatch(fieldChanged({ field: 'city', value: 'Reston' }))
    await store.dispatch(submitSearch())
    store.dispatch(fieldChanged({ field: 'city', value: 'Vienna' })) // typed, not submitted

    await store.dispatch(goToPage(2))

    expect(requestedUrls()[1]).toBe('/api/v1/listings?city=Reston&page=2&pageSize=5')
  })

  it('goToPage does nothing before any search has run', async () => {
    const store = makeStore()
    await store.dispatch(goToPage(2))
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('maps server validation errors onto the matching form fields', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        { status: 400, detail: 'page 9 is out of range', errors: [{ field: 'page', message: 'page 9 is out of range' }] },
        400,
      ),
    )
    const store = makeStore()

    await store.dispatch(runSearch({ page: 9, pageSize: 5 }))

    const state = store.getState().search
    expect(state.status).toBe('failed')
    expect(state.error).toBe('page 9 is out of range')
    expect(state.fieldErrors.page).toBe('page 9 is out of range')
  })

  it('loadCities stores suggestions and tolerates failure', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(['Reston']))
    const store = makeStore()
    await store.dispatch(loadCities())
    expect(store.getState().search.cities).toEqual(['Reston'])

    fetchMock.mockRejectedValueOnce(new TypeError('offline'))
    await store.dispatch(loadCities())
    expect(store.getState().search.cities).toEqual(['Reston'])
    expect(store.getState().search.status).toBe('idle')
  })
})
