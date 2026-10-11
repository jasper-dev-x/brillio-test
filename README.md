# Listing Search Service

A full-stack search experience over property listings ingested from multiple MLS feeds:
filter, rank by relevance, paginate.

| Layer | Stack |
|---|---|
| Backend | Java 17+, Spring Boot 3.5 (Web, Validation, Actuator), springdoc-openapi, Maven Wrapper |
| Frontend | React 19 + TypeScript, Redux Toolkit, Tailwind CSS 4 + daisyUI 5 (bundled with Vite) |
| Backend tests | JUnit 5, AssertJ, Mockito, Spring MockMvc (`@WebMvcTest` slice + `@SpringBootTest`) |
| Frontend tests | **Jest** + React Testing Library (unit/integration), **Cypress** (component), **Playwright** (end-to-end) |

## Quick start

Prerequisites: **JDK 17 or newer** and **Node 22.12+**. Maven is not required; the project ships the Maven Wrapper (`backend/mvnw`).

```bash
./dev.sh     # API on :8080, UI on :5173 → open http://localhost:5173
./test.sh    # every suite: backend, Jest, Cypress, Playwright
```

`dev.sh`:

- checks Java, Node and free ports
- installs npm dependencies on first run
- starts Spring Boot and waits for `/actuator/health`
- then starts Vite

Logs are prefixed `[api]` / `[web]`. **Ctrl+C stops both.** Ports can be overridden: `BACKEND_PORT=9090 FRONTEND_PORT=3000 ./dev.sh`.

Swagger UI: http://localhost:8080/swagger-ui.html · OpenAPI JSON: `/v3/api-docs`

### Tests

```bash
./test.sh backend     # ./mvnw verify — unit, web-slice and integration tests
./test.sh unit        # ESLint + tsc (app, Jest, Cypress, Playwright configs) + Jest
./test.sh component   # Cypress component tests, headless
./test.sh e2e         # Playwright; starts API + UI itself, or reuses ./dev.sh if running
```

From `frontend/` you can also run:

- `npm test`, `npm run test:coverage`
- `npm run test:component:open` (interactive Cypress)
- `npm run test:e2e:ui` (Playwright UI mode)

| Suite | Tool | What it covers |
|---|---|---|
| `RelevanceScorerTest` | JUnit/AssertJ | budget curve and tolerances, recency half-life, future/missing dates, blending, configurable weights |
| `DuplicateDetectorTest` | JUnit/AssertJ | address normalization, cross-feed match, different units/cities, id collisions |
| `DefaultListingSearchServiceTest` | JUnit/AssertJ | each filter and combinations, no matches, unknown vs filtered-out city, ranking, **tied scores**, **pagination boundaries** |
| `ListingSearchRequestValidationTest` | Jakarta Validation | every rule, min = max allowed, all violations reported together |
| `ListingMapperTest`, `JsonFileListingRepositoryTest` | JUnit/AssertJ | DTO mapping and defaults; data loading, fail-fast on a missing file |
| `ListingControllerTest` | `@WebMvcTest` + Mockito | binding, normalization, 400 problem responses (incl. type mismatch), 500 without leaking internals |
| `ListingSearchApiIntegrationTest` | `@SpringBootTest` + MockMvc | real data with a pinned clock, 404 problem format, health, OpenAPI |
| `src/**/*.test.ts(x)` | Jest + RTL | form validation, API client errors, Redux reducers/thunks (stale-response guard), full-app states |
| `cypress/component/*.cy.tsx` | Cypress | `Pagination`, `ResultsList`, `SearchForm` (query string, inline errors), `ResultsPanel` (loading/empty/error/paging) in a real browser |
| `e2e/search.spec.ts` | Playwright | browser → Vite → Spring Boot: filtering, ranking, pagination without repeats, validation, unknown city, duplicates, API problem contract |

## API

### `GET /api/v1/listings`

All parameters are optional.

| Param | Type | Rule |
|---|---|---|
| `minPrice`, `maxPrice` | number | ≥ 0, `minPrice ≤ maxPrice`, inclusive |
| `minBedrooms` | integer | ≥ 0, inclusive |
| `city` | string | ≤ 100 chars; case-insensitive exact match |
| `keyword` | string | ≤ 100 chars; **every** word must appear in `description` (case-insensitive) |
| `targetBudget` | number | > 0; turns on budget-fit ranking |
| `status` | string | `active` \| `pending` \| `sold` (any case) |
| `page` | integer | ≥ 1, default 1; must be ≤ `totalPages` when there are results |
| `pageSize` | integer | 1–50, default 5 (`listings.search.default-page-size`) |

```jsonc
// GET /api/v1/listings?city=springfield&targetBudget=450000&pageSize=2
{
  "items": [{
    "id": "A1", "source": "MLS_A", "address": "123 Main St, Apt 4B", "price": 450000, "bedrooms": 2, /* … */
    "score": 95.53,                                   // 0–100; recency depends on today's date
    "scoreBreakdown": { "budget": 1.0, "recency": 0.85 },
    "possibleDuplicates": [{ "id": "B7", "source": "MLS_B", "address": "123 Main Street, Unit 4B", "price": 452000 }]
  }],
  "page": 1, "pageSize": 2, "totalItems": 4, "totalPages": 2, "notices": []
}
```

`GET /api/v1/cities` returns the distinct cities, which the UI uses for autocomplete.

### Errors: RFC 9457 `application/problem+json`

```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Invalid request parameters",
  "status": 400,
  "detail": "2 invalid search parameters",
  "errors": [
    { "field": "minPrice", "message": "minPrice cannot be greater than maxPrice" },
    { "field": "pageSize", "message": "pageSize must be 1 or greater" }
  ]
}
```

| Situation | Response |
|---|---|
| Rule violations (min > max, negatives, `pageSize ≤ 0` or > 50, `page < 1`, `targetBudget ≤ 0`, bad `status`, text too long) | 400, **every** bad field listed |
| Non-numeric value (`?minPrice=cheap`) | 400: `minPrice must be a number (got 'cheap')`, not a 500 |
| `page` past the last page | 400 on field `page` |
| `city` not in the data | **200** with empty `items` and a `notices` entry listing valid cities |
| Valid filters, no matches | 200 with empty `items` and no notice |
| Unknown path / unexpected error | 404 / 500 problem; the 500 never leaks exception details |

**Why an unknown city returns 200:** the request is well-formed and "nothing matches" is a legitimate answer. The notice lets the user tell a typo apart from filters that are too narrow.

## Relevance scoring

The score has two components, each scaled 0–1, blended into a **0–100** score. Parameters live in `application.yml` under `listings.scoring` and are validated at startup.

**Budget fit** (only when `targetBudget` is set) is a linear falloff that is **asymmetric**:

- **Under budget:** 10% under scores 0.8, reaching 0 at 50% under.
- **Over budget:** 10% over scores 0.6, reaching 0 at 25% over.

Going over budget may make a home unaffordable, while going under does not.

**Recency** decays exponentially with a **30-day half-life**: today = 1.0, 30 days = 0.5, 60 days = 0.25. It never reaches zero, so recency still orders old listings among themselves.

```
score = 100 × (0.7 × budgetFit + 0.3 × recency) / (0.7 + 0.3)    // with targetBudget
score = 100 × recency                                            // without
```

Budget gets the higher weight because it is an explicit user preference; recency is an implicit freshness signal. Every result returns `scoreBreakdown`, and the UI shows it on hover, so each ranking can be explained.

**Ties.** Scores are rounded to 2 decimals before sorting, so floating-point noise doesn't decide the order. Ties are then broken by:

1. Newer `listedDate`
2. Lower price
3. `source`
4. `id`

That gives a total, deterministic order: the same query always produces the same pages, and no listing appears on two pages (the Playwright suite checks this).

"Today" comes from an injected `java.time.Clock`. Tests pin it; production uses the system clock.

## Cross-feed duplicates

The same property often arrives from both feeds with different formatting ("123 Main St, Apt 4B" vs "123 Main Street, Unit 4B") or even a different zip (22150 vs 22151). `DuplicateDetector` normalizes the address:

- lower-cases it and strips punctuation
- maps street suffixes and directionals to USPS abbreviations
- collapses `apt`/`unit`/`suite`/`#`

It then groups listings on normalized address + city, **ignoring zip**.

Duplicates are **flagged, not merged**. Each result carries `possibleDuplicates`, and the UI shows a badge. Merging would mean deciding whose price, date and description wins, which is a business rule; flagging keeps every source visible. Groups are computed against the full dataset, so a listing is flagged even when its twin is filtered out.

## Architecture

```
backend/src/main/java/com/listings/search
├── controller/   ListingController — GET /api/v1/listings, /api/v1/cities (+ OpenAPI annotations)
├── dto/          ListingSearchRequest (Bean Validation + @ValidPriceRange), response records
├── mapper/       ListingMapper — DTO ⇄ domain, applies defaults/normalization
├── service/      ListingSearchService (interface) → DefaultListingSearchService
│   ├── scoring/  RelevanceScorer
│   └── dedup/    DuplicateDetector
├── repository/   ListingRepository (interface) → JsonFileListingRepository
├── model/        Listing, ListingStatus, SearchCriteria, SearchResult, … (immutable records)
├── exception/    GlobalExceptionHandler (ResponseEntityExceptionHandler → ProblemDetail)
└── config/       ScoringProperties / SearchProperties (@ConfigurationProperties), Clock, CORS, OpenAPI

frontend/
├── src/api/            typed fetch client + contract types
├── src/app/            Redux store factory + typed hooks
├── src/features/search searchSlice (RTK), form model/validation, SearchForm, ResultsPanel, ResultsList, Pagination
├── src/**/*.test.tsx   Jest
├── cypress/component/  Cypress component specs (mounted inside a Redux Provider)
└── e2e/                Playwright specs + page object
```

**Request flow:**

1. **Bind and validate.** `@Valid @ModelAttribute` binds the query string to the DTO and runs Bean Validation. Any failure, including a type mismatch, surfaces as a `MethodArgumentNotValidException`, which `GlobalExceptionHandler` turns into a problem response.
2. **Map.** The mapper normalizes the DTO into an immutable `SearchCriteria`.
3. **Search.** The service filters, scores, sorts and paginates.
4. **Respond.** The mapper builds the response DTO.

Domain objects never leak into the API layer, and DTOs never reach the service.

## Design notes & trade-offs

- **Data access.** The data is in memory, loaded once from JSON behind a `ListingRepository` interface. A JPA/JDBC or search-index implementation can replace it without touching the service. At real scale, filtering and pagination would move into the store (for example an OpenSearch function score).
- **Offset pagination** is simple and correct for a static dataset. With live ingestion, keyset pagination on the sort tuple would keep pages stable.
- **Keyword search** is a substring AND match. Its known limitation is that "pets" also matches "no pets"; fixing that needs negation-aware parsing or structured fields the data doesn't have.
- **Money is `BigDecimal`** end to end in the backend. Scoring converts to double only for the ratio math.
- **Validation runs on both sides.** The client validates for instant feedback, and the server is authoritative: its field errors map back onto the form inputs.
- **Redux state** keeps the live form (raw strings, so bad input can be shown back) separate from the *applied* search. Pagination reuses the applied params, and responses from superseded requests are discarded by request id.
- **Spring Boot 3.5.x** was chosen as the widely deployed, stable line. Moving to Boot 4 mainly means Jackson 3 and relocated test annotations.
