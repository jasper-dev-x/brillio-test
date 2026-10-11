import { describe, expect, it } from 'vitest'

import { initialFormValues, toSearchParams, validateForm, type SearchFormValues } from './formModel'

const form = (overrides: Partial<SearchFormValues>): SearchFormValues => ({ ...initialFormValues, ...overrides })

describe('validateForm', () => {
  it('accepts the default form', () => {
    expect(validateForm(initialFormValues)).toEqual({})
  })

  it('rejects minPrice greater than maxPrice', () => {
    expect(validateForm(form({ minPrice: '600000', maxPrice: '400000' }))).toEqual({
      minPrice: 'Min price cannot be greater than max price',
    })
  })

  it('allows minPrice equal to maxPrice', () => {
    expect(validateForm(form({ minPrice: '500000', maxPrice: '500000' }))).toEqual({})
  })

  it('rejects non-numeric and negative values', () => {
    const errors = validateForm(form({ minPrice: 'abc', minBedrooms: '-1', targetBudget: '1.5' }))
    expect(Object.keys(errors).sort()).toEqual(['minBedrooms', 'minPrice', 'targetBudget'])
  })

  it('accepts currency formatting like $450,000', () => {
    expect(validateForm(form({ minPrice: '$450,000' }))).toEqual({})
    expect(toSearchParams(form({ minPrice: '$450,000' })).minPrice).toBe(450000)
  })

  it('rejects zero target budget', () => {
    expect(validateForm(form({ targetBudget: '0' })).targetBudget).toMatch(/greater than 0/)
  })

  it('rejects page size of zero, empty, or above the max', () => {
    expect(validateForm(form({ pageSize: '0' })).pageSize).toBeDefined()
    expect(validateForm(form({ pageSize: '' })).pageSize).toBeDefined()
    expect(validateForm(form({ pageSize: '51' })).pageSize).toBeDefined()
  })
})

describe('toSearchParams', () => {
  it('omits blank fields and trims text', () => {
    expect(toSearchParams(form({ city: '  Reston ', keyword: '   ' }), 3)).toEqual({
      minPrice: undefined,
      maxPrice: undefined,
      minBedrooms: undefined,
      targetBudget: undefined,
      city: 'Reston',
      keyword: undefined,
      status: undefined,
      page: 3,
      pageSize: 5,
    })
  })
})
