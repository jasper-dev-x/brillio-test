import type { ListingStatus, SearchParams } from '../../api/types'

/**
 * Form values are kept as raw strings (exactly what the user typed) so an
 * invalid entry like "12abc" can be shown back with an error instead of being
 * silently coerced. They're converted to typed SearchParams only on submit.
 */
export interface SearchFormValues {
  minPrice: string
  maxPrice: string
  minBedrooms: string
  city: string
  keyword: string
  targetBudget: string
  status: '' | ListingStatus
  pageSize: string
}

export type FormField = keyof SearchFormValues
export type FieldErrors = Partial<Record<FormField | 'page', string>>

export const MAX_PAGE_SIZE = 50
export const PAGE_SIZE_OPTIONS = ['5', '10', '20', '50'] as const

export const initialFormValues: SearchFormValues = {
  minPrice: '',
  maxPrice: '',
  minBedrooms: '',
  city: '',
  keyword: '',
  targetBudget: '',
  status: '',
  pageSize: '5',
}

const WHOLE_NUMBER = /^\d+$/

/** Strips thousands separators and "$" so "450,000" and "$450000" are accepted. */
function cleanNumber(raw: string): string {
  return raw.trim().replace(/[$,\s]/g, '')
}

function parseWholeNumber(raw: string): number | undefined {
  const cleaned = cleanNumber(raw)
  return cleaned === '' ? undefined : Number(cleaned)
}

/**
 * Client-side validation mirrors the server rules for fast feedback.
 * The server remains the source of truth: its 400 responses are surfaced too.
 */
export function validateForm(values: SearchFormValues): FieldErrors {
  const errors: FieldErrors = {}

  const numericFields: Array<[FormField, string]> = [
    ['minPrice', 'Min price'],
    ['maxPrice', 'Max price'],
    ['minBedrooms', 'Min bedrooms'],
    ['targetBudget', 'Target budget'],
    ['pageSize', 'Page size'],
  ]
  for (const [field, label] of numericFields) {
    const cleaned = cleanNumber(values[field])
    if (cleaned !== '' && !WHOLE_NUMBER.test(cleaned)) {
      errors[field] = `${label} must be a whole number of 0 or more`
    }
  }

  const minPrice = errors.minPrice ? undefined : parseWholeNumber(values.minPrice)
  const maxPrice = errors.maxPrice ? undefined : parseWholeNumber(values.maxPrice)
  if (minPrice !== undefined && maxPrice !== undefined && minPrice > maxPrice) {
    errors.minPrice = 'Min price cannot be greater than max price'
  }

  if (!errors.targetBudget && parseWholeNumber(values.targetBudget) === 0) {
    errors.targetBudget = 'Target budget must be greater than 0'
  }

  if (!errors.pageSize) {
    const pageSize = parseWholeNumber(values.pageSize)
    if (pageSize === undefined || pageSize < 1) {
      errors.pageSize = 'Page size must be at least 1'
    } else if (pageSize > MAX_PAGE_SIZE) {
      errors.pageSize = `Page size cannot exceed ${MAX_PAGE_SIZE}`
    }
  }

  return errors
}

/** Converts already-validated form values into API params. */
export function toSearchParams(values: SearchFormValues, page = 1): SearchParams {
  const text = (s: string) => (s.trim() === '' ? undefined : s.trim())
  return {
    minPrice: parseWholeNumber(values.minPrice),
    maxPrice: parseWholeNumber(values.maxPrice),
    minBedrooms: parseWholeNumber(values.minBedrooms),
    targetBudget: parseWholeNumber(values.targetBudget),
    city: text(values.city),
    keyword: text(values.keyword),
    status: values.status === '' ? undefined : values.status,
    page,
    pageSize: parseWholeNumber(values.pageSize) ?? 5,
  }
}
