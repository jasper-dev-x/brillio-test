import { expect, type Locator, type Page } from '@playwright/test'

/** Page object for the listing search screen: keeps selectors in one place. */
export class SearchPage {
  readonly page: Page
  readonly results: Locator
  readonly rows: Locator
  readonly summary: Locator
  readonly searchButton: Locator
  readonly resetButton: Locator

  constructor(page: Page) {
    this.page = page
    this.results = page.getByRole('table', { name: 'Search results' })
    this.rows = this.results.locator('tbody tr')
    this.summary = page.getByText(/^Showing \d+–\d+ of \d+ listings|^No results$/)
    this.searchButton = page.getByRole('button', { name: 'Search', exact: true })
    this.resetButton = page.getByRole('button', { name: 'Reset' })
  }

  async goto() {
    await this.page.goto('/')
    await expect(this.results).toBeVisible()
  }

  field(label: string): Locator {
    return this.page.getByLabel(label, { exact: true })
  }

  async fill(values: Partial<Record<'Keyword' | 'City' | 'Min price' | 'Max price' | 'Min bedrooms' | 'Target budget', string>>) {
    for (const [label, value] of Object.entries(values)) {
      await this.field(label).fill(value ?? '')
    }
  }

  /** Clicks Search and waits for the API response so assertions see fresh results. */
  async search() {
    const response = this.page.waitForResponse((r) => r.url().includes('/api/v1/listings'))
    await this.searchButton.click()
    return response
  }

  async goToPage(label: 'Next page' | 'Previous page' | `Page ${number}`) {
    const response = this.page.waitForResponse((r) => r.url().includes('/api/v1/listings'))
    await this.page.getByRole('button', { name: label, exact: true }).click()
    return response
  }

  row(index: number): Locator {
    return this.rows.nth(index)
  }

  /** "MLS_A #A1"-style source/id labels; unique per listing (addresses can repeat across feeds). */
  async listingIds(): Promise<string[]> {
    return this.rows.locator('.badge.font-mono').allInnerTexts()
  }

  async addresses(): Promise<string[]> {
    return this.rows.locator('td:nth-child(2) > div:first-child').allInnerTexts()
  }
}
