import { Pagination } from '../../src/features/search/Pagination'

const byLabel = (label: string) => cy.get(`[aria-label="${label}"]`)

describe('<Pagination />', () => {
  it('renders nothing when everything fits on one page', () => {
    cy.mount(<Pagination page={1} totalPages={1} onChange={cy.stub()} />)
    cy.get('nav[aria-label="Pagination"]').should('not.exist')
  })

  it('disables Previous on the first page and marks the current page', () => {
    cy.mount(<Pagination page={1} totalPages={3} onChange={cy.stub()} />)
    byLabel('Previous page').should('be.disabled')
    byLabel('Next page').should('not.be.disabled')
    byLabel('Page 1').should('have.attr', 'aria-current', 'page')
  })

  it('disables Next on the last page', () => {
    cy.mount(<Pagination page={3} totalPages={3} onChange={cy.stub()} />)
    byLabel('Next page').should('be.disabled')
  })

  it('reports the requested page', () => {
    const onChange = cy.stub().as('onChange')
    cy.mount(<Pagination page={2} totalPages={5} onChange={onChange} />)

    byLabel('Next page').click()
    cy.get('@onChange').should('have.been.calledWith', 3)
    byLabel('Previous page').click()
    cy.get('@onChange').should('have.been.calledWith', 1)
    byLabel('Page 5').click()
    cy.get('@onChange').should('have.been.calledWith', 5)
  })

  it('does not re-request the current page', () => {
    const onChange = cy.stub().as('onChange')
    cy.mount(<Pagination page={2} totalPages={3} onChange={onChange} />)
    byLabel('Page 2').click()
    cy.get('@onChange').should('not.have.been.called')
  })

  it('collapses long ranges with an ellipsis', () => {
    cy.mount(<Pagination page={5} totalPages={10} onChange={cy.stub()} />)
    cy.get('nav[aria-label="Pagination"]').contains('…').should('exist')
    byLabel('Page 1').should('exist')
    byLabel('Page 10').should('exist')
    byLabel('Page 2').should('not.exist')
  })

  it('locks every button while a request is in flight', () => {
    cy.mount(<Pagination page={2} totalPages={3} disabled onChange={cy.stub()} />)
    byLabel('Page 1').should('be.disabled')
    byLabel('Next page').should('be.disabled')
  })
})
