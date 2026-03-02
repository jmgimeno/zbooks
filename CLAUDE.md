# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

Three terminals are required for local development:

```bash
# Terminal 1 — backend (auto-restart on source changes)
sbt "~backend/reStart"

# Terminal 2 — frontend (fast Scala.js link on source changes)
sbt "~frontend/fastLinkJS"

# Terminal 3 — Tailwind CSS watch
cd frontend-assets && npx @tailwindcss/cli -i css/input.css -o dist/styles.css --watch
```

Point the backend at the dev assets:
```bash
export ZBOOKS_STATIC_DIR=/path/to/zbooks/modules/frontend/target/scala-3.8.2/frontend-fastopt
```

The dev fastopt directory does not include `styles.css`; copy or symlink `frontend-assets/dist/styles.css` into it.

**Build for production:**
```bash
docker build -t zbooks .
docker run -p 8080:8080 -v zbooks-data:/data zbooks
```

**SBT compile only (no tests exist yet):**
```bash
sbt compile          # all modules
sbt backend/compile
sbt frontend/compile
```

## Architecture

### Module structure

Three SBT modules:

- **`shared`** — `crossProject(JVMPlatform, JSPlatform)`. Contains all data models (`Book`, `Reading`, `NewBook`, `NewReading`) and API request/response types (`BookWithReadings`, `CreateBookRequest`, etc.) with `zio-json` codecs derived via `derives JsonCodec`. Dates are represented as ISO Strings (`String`, not `java.time.*`) to avoid the scala-java-time dependency on JS.

- **`backend`** — JVM-only ZIO HTTP server. Layer dependency chain: `AppConfig → DataSource (DatabaseSetup) → BookRepo/ReadingRepo → BookService/ReadingService → routes → Server`. All wiring is in `Main.scala`.

- **`frontend`** — Scala.js Laminar SPA. Hash routing (`#/books/:id`) — every non-API GET returns `index.html`. State is managed via Laminar `Var`s in `state/AppState.scala`.

### Backend layers

```
AppConfig          reads ZBOOKS_PORT / ZBOOKS_DB_PATH / ZBOOKS_STATIC_DIR from env
DatabaseSetup      HikariCP pool + runs schema.sql on startup (CREATE TABLE IF NOT EXISTS)
BookRepo           Magnum Repo[NewBookRow, BookRow, Long] wrapping DataSource
ReadingRepo        Magnum Repo[NewReadingRow, ReadingRow, Long]
BookService        business logic + validation (evaluation 1–5, year 1000–9999)
ReadingService     business logic for reading sessions
BookRoutes         ZIO HTTP Routes for /api/books/**
ReadingRoutes      ZIO HTTP Routes for /api/books/:id/readings/**
StaticRoutes       serves /assets/* and catch-all → index.html
```

### Database

H2 embedded file database. Table names follow Magnum's `CamelToSnakeCase` mapper: `BookRow` → `book_row`, `ReadingRow` → `reading_row`. JDBC URL includes `NON_KEYWORDS=YEAR` to un-reserve `YEAR` as an H2 2.x keyword. Schema is applied idempotently from `modules/backend/src/main/resources/schema.sql`.

### Frontend

- **Router** (`Router.scala`) — `Var[Page]` updated on `hashchange` events. Pages: `BookList` and `BookDetail(id)`.
- **AppState** (`state/AppState.scala`) — global `Var`s: `booksVar`, `loadingVar`, `errorVar`.
- **ApiClient** (`api/ApiClient.scala`) — `Future`-based, uses `dom.fetch`. Responses decoded with `zio-json`.
- Components: `BookListPage`, `BookDetailPage`, `BookForm`, `ReadingForm`, `StarRating`.

### Docker multi-stage build

1. `sbt-build` — compiles backend (`backend/stage`) and optimised frontend JS (`frontend/fullLinkJS`)
2. `css-build` — runs Tailwind CLI against the compiled JS (needed to scan for used classes) to produce minified `styles.css`
3. `runtime` — Alpine JRE 21 with the staged backend, `main.js`, `styles.css`, and `index.html`

## Known API Quirks

**Laminar:**
- Use `tpe := "button"` (not `typ`) for button type attribute
- `onInput.mapToValue --> someVar.writer` for text input binding
- Semantic HTML elements use `Tag` suffix: `navTag`, `mainTag`, `headerTag`, `footerTag`, `sectionTag`, `asideTag`, `articleTag`
- Avoid naming function parameters `placeholder` — it shadows the `L.placeholder` attribute; use `hint` instead

**scalajs-dom 2.8.x:**
- `RequestInit.method` expects `HttpMethod`, not `String` — cast with `.asInstanceOf[HttpMethod]`

**ZIO HTTP 3.x:**
- Route handlers with 2 path variables receive `(id1: Long, id2: Long, req: Request)`

**Magnum:**
- Use `connect(ds) { ... }` for auto-commit operations
- Pattern: `Repo[EntityCreate, EntityRead, IdType]`

**ZIO HTTP Server config:**
```scala
ZLayer.fromZIO(ZIO.service[AppConfig].map(cfg => Server.Config.default.port(cfg.port)))
```
