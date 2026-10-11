import type { SearchResponse } from '../../src/api/types'
import type { RootState } from '../../src/app/store'
import { ResultsPanel } from '../../src/features/search/ResultsPanel'
import { initialState, type SearchState } from '../../src/features/search/searchSlice'
import { item, page } from '../fixtures/listings'

function state(overrides: Partial<SearchState>): Partial<RootState> {
  return { search: { ...initialState, ...overrides } }
}

function loaded(result: SearchResponse): Partial<RootState> {
  return state({ status: 'succeeded', result, applied: { page: result.page, pageSize: result.pageSize } })
}

describe('<ResultsPanel />', () => {
  it('shows skeletons before the first response', () => {
    cy.mount(<ResultsPanel />, { preloadedState: state({ status: 'loading' }) })
    cy.get('[aria-label="Loading results"] .skeleton').should('have.length.at.least', 1)
  })

  it('summarizes the visible range of results', () => {
    cy.mount(<ResultsPanel />, {
      preloadedState: loaded(page({ items: [item(), item({ id: 'A2' })], page: 2, pageSize: 2, totalItems: 5, totalPages: 3 })),
    })
    cy.contains('Showing 3–4 of 5 listings, ranked by relevance')
  })

  it('shows the empty state with the server notice for an unknown city', () => {
    cy.mount(<ResultsPanel />, {
      preloadedState: loaded(
        page({ items: [], totalItems: 0, totalPages: 0, notices: ["No listings found in city 'Gotham'. Available cities: Reston."] }),
      ),
    })
    cy.contains('No listings match your search')
    cy.get('[role="status"]').should('contain', 'Gotham')
    cy.get('nav[aria-label="Pagination"]').should('not.exist')
  })

  it('shows the error state with a retry button', () => {
    cy.intercept('GET', '/api/v1/listings*', { body: page() }).as('retry')
    cy.mount(<ResultsPanel />, { preloadedState: state({ status: 'failed', error: 'Could not reach the listings service.' }) })

    cy.get('[role="alert"]').should('contain', 'Search failed').and('contain', 'Could not reach the listings service.')
    cy.contains('button', 'Retry').click()
    cy.wait('@retry')
    cy.get('table[aria-label="Search results"]').should('exist')
  })

  it('requests the next page of the applied search when paginating', () => {
    cy.intercept('GET', '/api/v1/listings*', (req) => {
      req.reply({ body: page({ items: [item({ id: 'P2', address: '2 Page St' })], page: 2, pageSize: 1, totalItems: 3, totalPages: 3 }) })
    }).as('page')
    cy.mount(<ResultsPanel />, {
      preloadedState: loaded(page({ items: [item({ address: '1 Page St' })], page: 1, pageSize: 1, totalItems: 3, totalPages: 3 })),
    })

    cy.get('[aria-label="Next page"]').click()

    cy.wait('@page').its('request.url').should('contain', 'page=2').and('contain', 'pageSize=1')
    cy.contains('2 Page St')
    cy.get('[aria-label="Page 2"]').should('have.attr', 'aria-current', 'page')
  })
})
