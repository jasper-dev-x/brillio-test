import { afterEach, describe, expect, it, vi } from 'vitest'

import { makeStore } from '../../app/store'
import { jsonResponse, makeResponse } from '../../test/fixtures'
import reducer, { fieldChanged, fieldErrorsSet, formReset, goToPage, initialState, runSearch, submitSearch } from './searchSlice'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('searchSlice reducers', () => {
  it('updates a field and clears its error', () => {
    const withError = reducer(initialState, fieldErrorsSet({ city: 'bad' }))
    const next = reducer(withError, fieldChanged({ field: 'city', value: 'Reston' }))
    expect(next.form.city).toBe('Reston')
    expect(next.fieldErrors.city).toBeUndefined()
  })

  it('editing either price bound clears both price errors', () => {
    const withErrors = reducer(initialState, fieldErrorsSet({ minPrice: 'x', maxPrice: 'y' }))
    const next = reducer(withErrors, fieldChanged({ field: 'maxPrice', value: '1' }))
    expect(next.fieldErrors).toEqual({})
  })

  it('formReset restores defaults', () => {
    const edited = reducer(initialState, fieldChanged({ field: 'keyword', value: 'pool' }))
    expect(reducer(edited, formReset()).form).toEqual(initialState.form)
  })

  it('ignores a stale response that resolves after a newer request started', () => {
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

describe('search thunks', () => {
  it('submitSearch blocks the request when the form is invalid', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const store = makeStore()
    store.dispatch(fieldChanged({ field: 'minPrice', value: '9' }))
    store.dispatch(fieldChanged({ field: 'maxPrice', value: '1' }))
    await store.dispatch(submitSearch())
    expect(fetchSpy).not.toHaveBeenCalled()
    expect(store.getState().search.fieldErrors.minPrice).toMatch(/greater than max/)
  })

  it('submitSearch always starts from page 1 and stores the result', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(makeResponse({ totalItems: 1 })))
    const store = makeStore()
    store.dispatch(fieldChanged({ field: 'city', value: 'Reston' }))
    await store.dispatch(submitSearch())
    expect(fetchSpy.mock.calls[0][0]).toBe('/api/listings?city=Reston&page=1&pageSize=5')
    expect(store.getState().search.status).toBe('succeeded')
    expect(store.getState().search.result?.totalItems).toBe(1)
  })

  it('goToPage reuses the applied search, not unsaved form edits', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => jsonResponse(makeResponse()))
    const store = makeStore()
    store.dispatch(fieldChanged({ field: 'city', value: 'Reston' }))
    await store.dispatch(submitSearch())
    store.dispatch(fieldChanged({ field: 'city', value: 'Vienna' })) // typed but not submitted
    await store.dispatch(goToPage(2))
    expect(fetchSpy.mock.calls[1][0]).toBe('/api/listings?city=Reston&page=2&pageSize=5')
  })

  it('server validation errors are surfaced on the matching field', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse({ status: 400, error: 'Bad Request', message: 'page 9 is out of range', details: [{ field: 'page', message: 'page 9 is out of range' }] }, 400),
    )
    const store = makeStore()
    await store.dispatch(runSearch({ page: 9, pageSize: 5 }))
    const s = store.getState().search
    expect(s.status).toBe('failed')
    expect(s.error).toBe('page 9 is out of range')
    expect(s.fieldErrors.page).toBe('page 9 is out of range')
  })
})
