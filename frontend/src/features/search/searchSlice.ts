import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit'

import { ApiRequestError, fetchCities, fetchListings } from '../../api/listingsApi'
import type { FieldError, SearchParams, SearchResponse } from '../../api/types'
import type { RootState } from '../../app/store'
import {
  initialFormValues,
  toSearchParams,
  validateForm,
  type FieldErrors,
  type FormField,
  type SearchFormValues,
} from './formModel'

export type RequestStatus = 'idle' | 'loading' | 'succeeded' | 'failed'

export interface SearchState {
  /** What's currently typed in the form (may differ from the last search run). */
  form: SearchFormValues
  fieldErrors: FieldErrors
  /** Params of the last search actually sent; pagination reuses these, not the live form. */
  applied: SearchParams | null
  status: RequestStatus
  result: SearchResponse | null
  error: string | null
  /** requestId of the newest in-flight search; older responses are ignored (no race conditions). */
  currentRequestId: string | null
  cities: string[]
}

export const initialState: SearchState = {
  form: initialFormValues,
  fieldErrors: {},
  applied: null,
  status: 'idle',
  result: null,
  error: null,
  currentRequestId: null,
  cities: [],
}

interface SearchFailure {
  message: string
  fieldErrors: FieldError[]
}

/** Low-level thunk: run a search with explicit params. */
export const runSearch = createAsyncThunk<SearchResponse, SearchParams, { rejectValue: SearchFailure }>(
  'search/run',
  async (params, { signal, rejectWithValue }) => {
    try {
      return await fetchListings(params, signal)
    } catch (e) {
      if (e instanceof ApiRequestError) {
        return rejectWithValue({ message: e.message, fieldErrors: e.fieldErrors })
      }
      throw e
    }
  },
)

/** Validate the form and, if valid, search from page 1. */
export const submitSearch = createAsyncThunk<void, void, { state: RootState }>(
  'search/submit',
  async (_, { getState, dispatch }) => {
    const { form } = getState().search
    const errors = validateForm(form)
    if (Object.keys(errors).length > 0) {
      dispatch(fieldErrorsSet(errors))
      return
    }
    dispatch(fieldErrorsSet({}))
    await dispatch(runSearch(toSearchParams(form, 1)))
  },
)

/** Move to another page of the last-applied search. */
export const goToPage = createAsyncThunk<void, number, { state: RootState }>(
  'search/goToPage',
  async (page, { getState, dispatch }) => {
    const { applied } = getState().search
    if (!applied) return
    await dispatch(runSearch({ ...applied, page }))
  },
)

export const loadCities = createAsyncThunk('search/loadCities', (_: void, { signal }) => fetchCities(signal))

const searchSlice = createSlice({
  name: 'search',
  initialState,
  reducers: {
    fieldChanged(state, action: PayloadAction<{ field: FormField; value: string }>) {
      const { field, value } = action.payload
      ;(state.form as unknown as Record<FormField, string>)[field] = value
      delete state.fieldErrors[field]
      // min/max price errors are about the pair, so editing either clears both
      if (field === 'minPrice' || field === 'maxPrice') {
        delete state.fieldErrors.minPrice
        delete state.fieldErrors.maxPrice
      }
    },
    fieldErrorsSet(state, action: PayloadAction<FieldErrors>) {
      state.fieldErrors = action.payload
    },
    formReset(state) {
      state.form = initialFormValues
      state.fieldErrors = {}
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(runSearch.pending, (state, action) => {
        state.status = 'loading'
        state.error = null
        state.currentRequestId = action.meta.requestId
        state.applied = action.meta.arg
      })
      .addCase(runSearch.fulfilled, (state, action) => {
        if (action.meta.requestId !== state.currentRequestId) return
        state.status = 'succeeded'
        state.result = action.payload
        state.currentRequestId = null
      })
      .addCase(runSearch.rejected, (state, action) => {
        if (action.meta.requestId !== state.currentRequestId) return
        state.status = 'failed'
        state.currentRequestId = null
        state.result = null
        if (action.payload) {
          state.error = action.payload.message
          // Map server-side field errors onto the form so the offending input is highlighted.
          for (const d of action.payload.fieldErrors) {
            ;(state.fieldErrors as Record<string, string>)[d.field] = d.message
          }
        } else {
          state.error = action.error.message ?? 'Unexpected error'
        }
      })
      .addCase(loadCities.fulfilled, (state, action) => {
        state.cities = action.payload
      })
    // loadCities failure is non-fatal: the city input still works without suggestions.
  },
})

export const { fieldChanged, fieldErrorsSet, formReset } = searchSlice.actions
export default searchSlice.reducer
