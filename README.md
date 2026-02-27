# ZBooks

A personal book management web application — track your library and reading sessions.

Built as a single-binary, single-container SPA: the Scala backend serves both the REST API and the compiled Scala.js frontend.

## Features

- Add, edit and delete books (title, author, publisher, year, 1–5 star rating)
- Record reading sessions (start date / end date) per book
- Live search across your library
- Persistent H2 embedded database (survives container restarts via a Docker volume)

## Tech stack

| Layer | Technology |
|---|---|
| Language | Scala 3.8.2 |
| Backend | ZIO 2 · ZIO HTTP 3.8 · Magnum 1.3 (ORM) · HikariCP · H2 |
| Frontend | Scala.js 1.20 · Laminar 17 · scalajs-dom 2.8 |
| Styling | Tailwind CSS 4 |
| Build | SBT 1.12.4 · sbt-native-packager · sbt-revolver |
| Runtime | Docker — Alpine JRE 21 (~136 MB image) |

## Project structure

```
zbooks/
├── build.sbt                        # Three-module SBT build
├── project/
│   ├── build.properties             # SBT version
│   └── plugins.sbt
├── modules/
│   ├── shared/                      # Cross-compiled JVM + JS
│   │   └── src/main/scala/zbooks/shared/
│   │       ├── models/              # Book, Reading, NewBook, NewReading
│   │       └── api/                 # Request/response types (zio-json codecs)
│   ├── backend/                     # JVM — ZIO HTTP server
│   │   └── src/main/scala/zbooks/backend/
│   │       ├── Main.scala           # Entry point / ZIO layer wiring
│   │       ├── AppConfig.scala      # Port, DB path, static dir (from env vars)
│   │       ├── db/                  # Magnum repos (BookRepo, ReadingRepo)
│   │       ├── service/             # Business logic (BookService, ReadingService)
│   │       └── http/                # Route handlers + static file serving
│   └── frontend/                    # Scala.js — Laminar SPA
│       └── src/main/scala/zbooks/frontend/
│           ├── Main.scala           # Laminar entry point
│           ├── Router.scala         # Hash-based routing (#/books/:id)
│           ├── state/               # Shared reactive state (AppState)
│           ├── api/                 # Fetch-based API client
│           └── components/          # Pages and UI components
├── frontend-assets/
│   ├── index.html                   # SPA shell
│   └── css/input.css                # Tailwind CSS entry point
├── Dockerfile                       # Multi-stage build
└── .dockerignore
```

### Shared module

The `shared` cross-project is compiled for both JVM (used by the backend) and JS (used by the frontend). It contains all the data models and API request/response types, with `zio-json` codecs derived automatically.

## Running with Docker (recommended)

### Build the image

```bash
docker build -t zbooks .
```

The multi-stage Dockerfile:
1. **sbt-build** — compiles the backend JAR (`backend/stage`) and the optimised frontend JS (`frontend/fullLinkJS`) inside `sbtscala/scala-sbt:eclipse-temurin-21.0.8_9_1.12.4_3.8.2`
2. **css-build** — runs `@tailwindcss/cli` against the compiled JS to produce a minified `styles.css`
3. **runtime** — copies the staged backend, the JS bundle and the CSS into a minimal Alpine JRE 21 image

### Run

```bash
docker run -p 8080:8080 -v zbooks-data:/data zbooks
```

Open [http://localhost:8080](http://localhost:8080).

The H2 database is stored at `/data/zbooks.mv.db` inside the container. The named volume `zbooks-data` persists it across restarts.

### Configuration (environment variables)

| Variable | Default | Description |
|---|---|---|
| `ZBOOKS_PORT` | `8080` | HTTP port |
| `ZBOOKS_DB_PATH` | `/data/zbooks` | H2 file path (without `.mv.db`) |
| `ZBOOKS_STATIC_DIR` | `/app/static` | Directory served as static assets |

## Local development

You need **SBT 1.12.4**, **Node.js 22+** and **Java 21+**.

Open three terminals in the project root:

**Terminal 1 — backend (auto-restart on source changes)**
```bash
sbt "~backend/reStart"
```

**Terminal 2 — frontend (fast Scala.js link on source changes)**
```bash
sbt "~frontend/fastLinkJS"
```

**Terminal 3 — Tailwind CSS watch**
```bash
cd frontend-assets
npx @tailwindcss/cli -i css/input.css -o dist/styles.css --watch
```

Then set the environment variable so the backend serves the fast-linked JS and the dev CSS:

```bash
export ZBOOKS_STATIC_DIR=/path/to/zbooks/modules/frontend/target/scala-3.8.2/frontend-fastopt
```

> **Note:** The dev static dir layout differs from the production one. For development you can also copy/symlink `frontend-assets/dist/styles.css` into the fastopt directory as `styles.css`.

## REST API

All endpoints are under `/api`.

### Books

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/books` | List all books with their reading sessions |
| `POST` | `/api/books` | Create a book |
| `GET` | `/api/books/:id` | Get one book with its reading sessions |
| `PUT` | `/api/books/:id` | Update a book |
| `DELETE` | `/api/books/:id` | Delete a book (cascades to readings) |

**Book body (create/update):**
```json
{ "name": "The Pragmatic Programmer", "author": "David Thomas",
  "editor": "Addison-Wesley", "year": 1999, "evaluation": 5 }
```
`evaluation` is optional (1–5).

### Reading sessions

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/books/:bookId/readings` | Add a reading session |
| `PUT` | `/api/books/:bookId/readings/:id` | Update a reading session |
| `DELETE` | `/api/books/:bookId/readings/:id` | Delete a reading session |

**Reading body (create/update):**
```json
{ "startDate": "2024-01-10", "endDate": "2024-02-03" }
```
Dates are ISO-8601 strings (`YYYY-MM-DD`).
