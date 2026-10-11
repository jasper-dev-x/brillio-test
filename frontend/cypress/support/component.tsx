import type { ReactNode } from 'react'
import { mount, type MountOptions, type MountReturn } from 'cypress/react'
import { Provider } from 'react-redux'

import { makeStore, type AppStore, type RootState } from '../../src/app/store'
import '../../src/index.css'

interface ReduxMountOptions extends MountOptions {
  /** Partial state to seed the store with (e.g. a search result already loaded). */
  preloadedState?: Partial<RootState>
  /** Use an existing store instead of creating one. */
  store?: AppStore
}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /** Mounts a component inside a Redux Provider. Yields the store as `@store`. */
      mount(component: ReactNode, options?: ReduxMountOptions): Chainable<MountReturn>
    }
  }
}

Cypress.Commands.add('mount', (component: ReactNode, options: ReduxMountOptions = {}) => {
  const { preloadedState, store = makeStore(preloadedState), ...mountOptions } = options
  cy.wrap(store, { log: false }).as('store')
  return mount(<Provider store={store}>{component}</Provider>, mountOptions)
})
