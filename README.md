# Listing Search Service

A full-stack search experience over property listings ingested from multiple MLS feeds.

- **Backend:** Java 17+ / Spring Boot 3.5 REST API (`backend/`)
- **Frontend:** React 19 + TypeScript + Redux Toolkit, styled with Tailwind CSS 4 + daisyUI 5, built with Vite (`frontend/`)

## Quick start

Prerequisites: **JDK 17+**, **Maven 3.9+** (`brew install maven`), **Node 22.12+**.

```bash
./dev.sh          # starts API on :8080 and UI on :5173 — open http://localhost:5173
./test.sh         # runs backend (JUnit) + frontend (typecheck + Vitest) suites
```

`dev.sh` checks prerequisites and free ports, installs npm dependencies on first run, starts the backend, waits until it is healthy, then starts the UI. Both logs stream to one terminal, prefixed `[api]` and `[web]`. **Ctrl+C stops both.** Override ports with `BACKEND_PORT=9090 FRONTEND_PORT=3000 ./dev.sh`.

Run pieces individually:

```bash
cd backend  && mvn spring-boot:run      # API only
cd frontend && npm install && npm run dev   # UI only (proxies /api -> :8080)
cd backend  && mvn test
cd frontend && npm test
```

## API

### `GET /api/listings`

All parameters are optional.

| Param | Type | Rule |
|---|---|---|
| `minPrice`, `maxPrice` | integer | ≥ 0, `minPrice ≤ maxPrice`, inclusive bounds |
| `minBedrooms` | integer | ≥ 0, inclusive |
| `city` | string | case-insensitive exact match, trimmed |
| `keyword` | string | case-insensitive; **every** word must appear in `description` |
| `targetBudget` | integer | > 0; enables budget-fit scoring |
| `status` | string | `active` \| `pending` \| `sold` |
| `page` | integer | ≥ 1, default 1, must be ≤ `totalPages` when there are results |
| `pageSize` | integer | 1–50, default 5 |

```bash
curl 'localhost:8080/api/listings?city=springfield&targetBudget=500000&pageSize=3'
```

```jsonc
{
  "items": [{
    "id": "A2", "source": "MLS_A", "address": "456 Oak Ave", "price": 525000, "bedrooms": 3, /* ...all listing fields */
    "score": 68.18,              // recency (and so the score) depends on today's date
    "scoreBreakdown": { "budget": 0.8, "recency": 0.41 },
    "possibleDuplicates": [{ "id": "B8", "source": "MLS_B", "address": "456 Oak Avenue", "price": 527500 }]
  }],
  "page": 1, "pageSize": 3, "totalItems": 4, "totalPages": 2,
  "notices": []
}
```

### `GET /api/cities`

Distinct cities in the dataset, used for autocomplete in the UI.

### Errors

Invalid input returns **400** with every problem found, not just the first, so the UI can highlight each bad field:

```json
{ "status": 400, "error": "Bad Request",
  "message": "minPrice (600000) cannot be greater than maxPrice (400000)",
  "details": [{ "field": "minPrice", "message": "minPrice (600000) cannot be greater than maxPrice (400000)" }] }
```

| Situation | Response |
|---|---|
| `minPrice > maxPrice`, negative numbers, `pageSize ≤ 0` or `> 50`, `page < 1`, `targetBudget ≤ 0`, unknown `status` | 400 + field details |
| Non-numeric value (`?minPrice=abc`) | 400 + field details (not a 500) |
| `page` beyond the last page | 400: "page 9 is out of range; there are 2 pages" |
| `city` that doesn't exist in the data | **200**, empty `items`, plus a `notices` entry listing the valid cities |
| Valid filters that match nothing | 200, empty `items`, no notice |

**Why an unknown city returns 200 rather than 4xx:** the request is well-formed, and "no results" is a legitimate answer. The notice lets the user tell a typo ("Gotham") apart from filters that are simply too narrow, without treating either as a client error.

## Relevance scoring

Each listing gets two components, each normalized to 0–1, blended into a **0–100** score.

**Budget fit** (only when `targetBudget` is given) is a linear falloff based on how far the price is from the target, and it is **asymmetric**:

- **Under budget:** 10% under scores 0.8 and reaches 0 at 50% under.
- **Over budget:** 10% over scores 0.6 and reaches 0 at 25% over.

The asymmetry is deliberate. A home that is cheaper than planned is still affordable, while one over budget may not be.

**Recency** decays exponentially with a **30-day half-life**: listed today = 1.0, 30 days ago = 0.5, 60 days ago = 0.25. Exponential decay never reaches zero, so recency still orders older listings among themselves.

```
score = 100 × (0.7 × budgetFit + 0.3 × recency)   // with targetBudget
score = 100 × recency                             // without targetBudget
```

Budget is weighted higher because it is an explicit user preference. Recency is an implicit signal of freshness and availability. Each result exposes its component scores (`scoreBreakdown`), and the UI shows them on hover, so the ranking can be explained.

**Ties.** Scores are rounded to 2 decimals before sorting, so floating-point noise doesn't decide the order. Ties are then broken deterministically:

1. Newer `listedDate`
2. Lower price
3. `source`
4. `id`

A stable total order means the same query always produces the same pages, and no listing can appear on two pages.

**"Today"** comes from an injected `java.time.Clock`. Tests pin it to a fixed date, and production uses the system clock. Absolute scores therefore drift down over time while relative order stays the same.

## Cross-feed duplicates

The same property often appears in both feeds with different formatting, for example "123 Main St, Apt 4B" vs "123 Main Street, Unit 4B", or zip 22150 vs 22151. `DuplicateDetector` normalizes each address:

- lower-cases it and strips punctuation
- maps street suffixes and directionals to USPS abbreviations
- collapses `apt`/`unit`/`suite`/`#` into one token

It then groups listings by normalized address + city. **Zip is deliberately ignored** because the sample data disagrees on it.

Duplicates are **flagged, not merged**. Each result lists its `possibleDuplicates`, and the UI shows a badge. Merging would mean deciding whose price, date and description wins, which is a business rule that hasn't been defined. Flagging keeps every source visible, and merging can be added later on top of the same grouping. Duplicate groups are computed against the full dataset, so a listing is flagged even when its twin is filtered out.

## Design notes & trade-offs

- **Pipeline:** validate → filter → score → sort → paginate (`SearchService`). Scoring runs only on listings that pass the filters.
- **In-memory data.** `ListingRepository` loads the JSON once at startup. With about 12 records a database adds nothing, and the search layer depends only on `findAll()`, so a JPA or JDBC repository could replace it without changes. At real scale, filtering and pagination would move into the database or a search engine (Elasticsearch/OpenSearch), with scoring as a function score.
- **Offset pagination.** It is simple and fits a small static dataset. With live ingestion, inserts between requests shift pages, and cursor/keyset pagination on (score, listedDate, price, source, id) would avoid that.
- **Keyword search** is a substring AND match. Its known limitation is that `pets` also matches "no pets". Fixing that needs negation-aware parsing or structured fields such as `petsAllowed`, which the data doesn't have.
- **City matching** is exact (case- and whitespace-insensitive). Fuzzy matching and aliases are a natural extension; the unknown-city notice already lists valid options.
- **Validation lives in two places.** The UI validates for instant feedback, and the server is the source of truth. Server field errors map back onto the form inputs.
- **Frontend state.** The live form (raw strings, so bad input can be shown back) is kept separate from the *applied* search. Pagination reuses the applied params, so unsubmitted edits don't leak into page changes. Each request has an id, and late responses from superseded requests are ignored to prevent race conditions.

## Tests

**Backend** (`backend/src/test`, JUnit 5):

- `RelevanceScorerTest`: budget curve, both tolerances, clamping, recency half-life, future and missing dates, blending
- `SearchValidatorTest`: every invalid input, boundaries (min = max is allowed), all errors reported together
- `DuplicateDetectorTest`: normalization, cross-source match, different units, different cities, id collisions across sources
- `SearchServiceTest`: each filter, combined filters, no matches, unknown vs. filtered-out city, ranking, **tied scores** (date then price then source/id), **pagination boundaries** (partial last page, exact multiple, page past the end, page size 1, page size > total, empty result)
- `ListingControllerTest`: HTTP end to end on the real sample data, including the JSON shape and the 400 mappings for each invalid case

**Frontend** (`frontend/src/**/*.test.ts(x)`, Vitest + Testing Library):

- form validation and param mapping
- API client error handling (400 details, network down, non-JSON body)
- Redux reducers and thunks (stale-response guard, pagination reusing the applied params)
- full-app tests for the results, empty, error, inline-validation, pagination and duplicate-badge states

## Project layout

```
backend/
  src/main/java/com/listings/
    model/Listing.java              listing record (key = source:id)
    repository/ListingRepository    loads sample_listings.json
    search/SearchService            validate → filter → score → sort → paginate
    search/SearchValidator          input rules → InvalidSearchException
    search/RelevanceScorer          scoring formula (constants at top)
    search/DuplicateDetector        address normalization + grouping
    web/ListingController           GET /api/listings, /api/cities
    web/GlobalExceptionHandler      exceptions → JSON ApiError
  src/main/resources/data/sample_listings.json
frontend/
  src/api/                          typed API client + contract types
  src/app/                          Redux store + typed hooks
  src/features/search/              slice, form model, form, results, pagination
dev.sh / test.sh
```
