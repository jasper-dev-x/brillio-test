import { combineReducers, configureStore } from '@reduxjs/toolkit'

import searchReducer from '../features/search/searchSlice'

const rootReducer = combineReducers({
  search: searchReducer,
})

export type RootState = ReturnType<typeof rootReducer>

/** Factory so tests can create an isolated store, optionally pre-seeded. */
export function makeStore(preloadedState?: Partial<RootState>) {
  return configureStore({ reducer: rootReducer, preloadedState })
}

export const store = makeStore()

export type AppStore = ReturnType<typeof makeStore>
export type AppDispatch = AppStore['dispatch']
