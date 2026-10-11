import { SearchForm } from '../../src/features/search/SearchForm'
import { page } from '../fixtures/listings'

describe('<SearchForm />', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/v1/listings*', { body: page() }).as('search')
  })

  it('sends every filter as a query parameter, starting from page 1', () => {
    cy.mount(<SearchForm />)

    cy.get('#keyword').type('fenced yard')
    cy.get('#city').type('Springfield')
    cy.get('#minPrice').type('400,000')
    cy.get('#maxPrice').type('600000')
    cy.get('#minBedrooms').type('3')
    cy.get('#status').select('Active')
    cy.get('#targetBudget').type('500000')
    cy.get('#pageSize').select('10')
    cy.contains('button', 'Search').click()

    cy.wait('@search').then(({ request }) => {
      const url = new URL(request.url)
      expect(Object.fromEntries(url.searchParams)).to.deep.equal({
        minPrice: '400000',
        maxPrice: '600000',
        minBedrooms: '3',
        targetBudget: '500000',
        city: 'Springfield',
        keyword: 'fenced yard',
        status: 'active',
        page: '1',
        pageSize: '10',
      })
    })
  })

  it('blocks submission and highlights the field when min price exceeds max price', () => {
    cy.mount(<SearchForm />)

    cy.get('#minPrice').type('600000')
    cy.get('#maxPrice').type('400000')
    cy.contains('button', 'Search').click()

    cy.get('#minPrice').should('have.class', 'input-error').and('have.attr', 'aria-invalid', 'true')
    cy.get('#minPrice-error').should('have.text', 'Min price cannot be greater than max price')
    cy.get('@search.all').should('have.length', 0)
  })

  it('rejects non-numeric input and clears the error once corrected', () => {
    cy.mount(<SearchForm />)

    cy.get('#minBedrooms').type('two')
    cy.contains('button', 'Search').click()
    cy.get('#minBedrooms-error').should('contain', 'whole number')

    cy.get('#minBedrooms').clear().type('2')
    cy.get('#minBedrooms-error').should('not.exist')
  })

  it('offers city suggestions from the store', () => {
    cy.mount(<SearchForm />, {
      preloadedState: {
        search: {
          form: { minPrice: '', maxPrice: '', minBedrooms: '', city: '', keyword: '', targetBudget: '', status: '', pageSize: '5' },
          fieldErrors: {},
          applied: null,
          status: 'idle',
          result: null,
          error: null,
          currentRequestId: null,
          cities: ['Reston', 'Vienna'],
        },
      },
    })
    cy.get('#city-options option').should('have.length', 2).first().should('have.value', 'Reston')
  })

  it('Reset clears the form and searches again', () => {
    cy.mount(<SearchForm />)
    cy.get('#keyword').type('pool')
    cy.contains('button', 'Reset').click()

    cy.get('#keyword').should('have.value', '')
    cy.wait('@search').its('request.url').should('not.contain', 'keyword')
  })
})
