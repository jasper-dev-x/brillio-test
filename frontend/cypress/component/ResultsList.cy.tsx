import { ResultsList } from '../../src/features/search/ResultsList'
import { item } from '../fixtures/listings'

describe('<ResultsList />', () => {
  it('renders one row per listing with address, price, bedrooms and score', () => {
    cy.mount(
      <ResultsList
        startRank={6}
        items={[
          item({ id: 'A1', address: '123 Main St', price: 450000, bedrooms: 2, score: 91.24 }),
          item({ id: 'B8', source: 'MLS_B', address: '456 Oak Ave', price: 527500, bedrooms: 3, score: 66 }),
        ]}
      />,
    )

    cy.get('table[aria-label="Search results"] tbody tr').should('have.length', 2)
    cy.get('tbody tr').first().within(() => {
      cy.contains('td', '6') // rank continues from the previous page
      cy.contains('123 Main St')
      cy.contains('$450,000')
      cy.contains('91.2')
      cy.contains('MLS_A #A1')
    })
    cy.get('tbody tr').eq(1).should('contain', '456 Oak Ave').and('contain', '$527,500')
  })

  it('shows status badges', () => {
    cy.mount(<ResultsList startRank={1} items={[item({ status: 'pending' })]} />)
    cy.contains('.badge', 'pending').should('have.class', 'badge-warning')
  })

  it('flags possible duplicates with the other feed in the tooltip', () => {
    cy.mount(
      <ResultsList
        startRank={1}
        items={[item({ possibleDuplicates: [{ id: 'B7', source: 'MLS_B', address: '123 Main Street', price: 452000 }] })]}
      />,
    )
    cy.get('[data-testid="duplicate-badge"]')
      .should('be.visible')
      .parent()
      .should('have.attr', 'data-tip')
      .and('contain', 'MLS_B #B7')
  })

  it('explains the score in a tooltip, including when no budget was given', () => {
    cy.mount(
      <ResultsList startRank={1} items={[item({ scoreBreakdown: { budget: null, recency: 0.43 }, score: 43 })]} />,
    )
    cy.get('[aria-label="Relevance score 43"]')
      .closest('.tooltip')
      .should('have.attr', 'data-tip', 'Recency 43% (no target budget set)')
  })
})
