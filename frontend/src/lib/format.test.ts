
import { formatDate, formatPrice, pageWindow } from './format'

describe('pageWindow', () => {
  it('returns nothing when there are no pages', () => {
    expect(pageWindow(1, 0)).toEqual([])
  })

  it('lists every page when there are few', () => {
    expect(pageWindow(2, 3)).toEqual([1, 2, 3])
  })

  it('collapses distant pages into gaps', () => {
    expect(pageWindow(5, 10)).toEqual([1, null, 4, 5, 6, null, 10])
  })

  it('handles first and last page boundaries', () => {
    expect(pageWindow(1, 10)).toEqual([1, 2, null, 10])
    expect(pageWindow(10, 10)).toEqual([1, null, 9, 10])
  })
})

describe('formatters', () => {
  it('formats USD without cents', () => {
    expect(formatPrice(452000)).toBe('$452,000')
  })

  it('formats ISO dates without timezone shift', () => {
    expect(formatDate('2026-08-01')).toBe('Aug 1, 2026')
  })
})
