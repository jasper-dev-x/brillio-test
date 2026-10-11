const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })
const integer = new Intl.NumberFormat('en-US')

export const formatPrice = (n: number) => currency.format(n)
export const formatNumber = (n: number) => integer.format(n)

/** "2026-08-29" -> "Aug 29, 2026" without timezone drift (ISO dates parse as UTC). */
export function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number)
  if (!y || !m || !d) return iso
  return new Date(y, m - 1, d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

/** Page numbers to render, with null for "…" gaps: [1, null, 4, 5, 6, null, 10] */
export function pageWindow(current: number, total: number, radius = 1): Array<number | null> {
  if (total <= 0) return []
  const pages = new Set<number>([1, total])
  for (let p = current - radius; p <= current + radius; p++) {
    if (p >= 1 && p <= total) pages.add(p)
  }
  const sorted = [...pages].sort((a, b) => a - b)
  const out: Array<number | null> = []
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) out.push(null)
    out.push(p)
  })
  return out
}
