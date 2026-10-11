import type { ListingItem } from '../../api/types'
import { formatDate, formatNumber, formatPrice } from '../../lib/format'

const STATUS_BADGE: Record<string, string> = {
  active: 'badge-success',
  pending: 'badge-warning',
  sold: 'badge-neutral',
}

function ScoreCell({ item }: { item: ListingItem }) {
  const { budget, recency } = item.scoreBreakdown
  const breakdown =
    budget === null
      ? `Recency ${Math.round(recency * 100)}% (no target budget set)`
      : `Budget fit ${Math.round(budget * 100)}% · Recency ${Math.round(recency * 100)}%`
  return (
    <div className="tooltip tooltip-left" data-tip={breakdown}>
      <div className="flex flex-col items-end gap-1">
        <span className="font-mono text-base font-semibold tabular-nums" aria-label={`Relevance score ${item.score}`}>
          {item.score.toFixed(1)}
        </span>
        <progress className="progress progress-primary h-1.5 w-16" value={item.score} max={100} />
      </div>
    </div>
  )
}

function DuplicateBadge({ item }: { item: ListingItem }) {
  if (item.possibleDuplicates.length === 0) return null
  const others = item.possibleDuplicates
    .map((d) => `${d.source} #${d.id} (${formatPrice(d.price)})`)
    .join(', ')
  return (
    <div className="tooltip tooltip-right" data-tip={`Same property also listed as: ${others}`}>
      <span className="badge badge-warning badge-outline badge-sm gap-1" data-testid="duplicate-badge">
        Possible duplicate
      </span>
    </div>
  )
}

interface Props {
  items: ListingItem[]
  /** Rank of the first item on this page (1-based), for the # column. */
  startRank: number
}

export function ResultsList({ items, startRank }: Props) {
  return (
    <div className="overflow-x-auto">
      <table className="table table-sm" aria-label="Search results">
        <thead>
          <tr>
            <th className="w-10">#</th>
            <th>Address</th>
            <th className="text-right">Price</th>
            <th className="text-center">Beds</th>
            <th className="text-center">Baths</th>
            <th className="text-right">Sq ft</th>
            <th>Listed</th>
            <th className="text-right">Score</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item, i) => (
            <tr key={`${item.source}:${item.id}`} className="hover:bg-base-200/60 align-top">
              <td className="text-base-content/50 tabular-nums">{startRank + i}</td>
              <td className="min-w-64">
                <div className="font-medium">{item.address}</div>
                <div className="text-xs text-base-content/60">
                  {item.city}, {item.state} {item.zip}
                </div>
                <div className="mt-1 flex flex-wrap items-center gap-1">
                  <span className={`badge badge-sm ${STATUS_BADGE[item.status] ?? 'badge-ghost'}`}>{item.status}</span>
                  <span className="badge badge-ghost badge-sm font-mono">
                    {item.source} #{item.id}
                  </span>
                  <DuplicateBadge item={item} />
                </div>
                <p className="mt-1 max-w-md text-xs text-base-content/70">{item.description}</p>
              </td>
              <td className="text-right font-medium tabular-nums">{formatPrice(item.price)}</td>
              <td className="text-center tabular-nums">{item.bedrooms}</td>
              <td className="text-center tabular-nums">{item.bathrooms}</td>
              <td className="text-right tabular-nums">{formatNumber(item.sqft)}</td>
              <td className="whitespace-nowrap">{formatDate(item.listedDate)}</td>
              <td className="text-right">
                <ScoreCell item={item} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
