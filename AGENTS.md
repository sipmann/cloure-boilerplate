# AGENTS.md

Clojure microservice boilerplate (btc wallet example) built on
`com.github.parenthesin/components`: schema, component, pedestal, reitit,
next-jdbc/hikaricp, postgres. See `README.md` for the full stack/feature list
and directory structure.

## Running things

- Run all tests: `bb test` (wraps `clj -M:test`, runs kaocha).
  - Unit only: `clj -M:test :unit` — Integration only: `clj -M:test :integration`.
  - Integration tests spin up an embedded Postgres (`pg-embedded-clj` /
    `io.zonky.test.embedded-postgres`) per run; this is slow-ish but works on
    this Windows machine. A benign `EmbeddedPostgres ... failed to stop
    postmaster` / `IllegalStateException` is printed after the summary line on
    shutdown — ignore it, it doesn't affect the test result.
  - The embedded Postgres runs on port 5433, not the default 5432 — the `test`
    task in `bb.edn` sets `DB-PORT=5433` via `:extra-env` (only for that child
    process), and `test/integration/microservice_boilerplate/util.clj` reads
    the same var to start `pg-embedded-clj` on that port. This means
    integration tests work even with a real native Postgres already running
    locally on 5432 — no need to stop it before running `bb test`. If you run
    `clj -M:test :integration` directly (bypassing `bb test`), set `DB-PORT`
    yourself first, or it'll default to 5432 and may collide with a local
    Postgres.
- Start the app: `bb run` (or `clj -M:dev`). REPL: `bb nrepl` / `bb cider`.
- Format: `bb format` (cljstyle) or `clj -M:clojure-lsp format`.
- Build uberjar: `bb uberjar`.
- Migrations (`:migratus` alias, `parenthesin.helpers.migrations`, files in
  `resources/migrations`):
  - Run pending migrations: `bb migrate` (or `clj -M:migratus migrate`).
  - Rollback latest: `bb rollback` (or `clj -M:migratus rollback`).
  - Create a new migration: `bb create-migration <name>` (or
    `clj -M:migratus create <name>`) — generates timestamped `.up.sql` /
    `.down.sql` stubs.

## Test layout

- `test/unit/...` — plain `clojure.test`.
- `test/integration/...` — `state-flow` flows against a real (embedded)
  Postgres + pedestal service, driven through
  `parenthesin.helpers.state-flow.server.pedestal/request!`.
- After changing anything under `test/integration`, run `bb test` and
  actually read the failure output — kaocha's stack traces are long, the
  useful bit is the last `Flow "..." failed with exception` block plus the
  final exception line.

## Known gotchas (learned the hard way)

- **`parenthesin.helpers.state-flow.server.pedestal/parsed-response`
  (components 0.4.3) NPEs on responses without a `Content-Type` header**
  (e.g. plain `{:status 302 :headers {"Location" ...}}` redirects), because it
  does `(string/includes? (get headers "Content-Type") "application/json")`
  with no nil-guard. Workaround used in this repo: always set
  `"Content-Type"` explicitly on redirect responses in
  `src/microservice_boilerplate/controllers/authentication.clj`. See `docs/TODO.md` — an
  upstream PR to fix `parsed-response` itself is still pending evaluation.
- **`request!` doesn't keep a cookie jar** — each call is isolated (it's
  `io.pedestal.test/response-for` under the hood), so integration tests must
  manually thread the session cookie and CSRF token between requests. `
  Set-Cookie` comes back as a *list* (`first` it before splitting on `;`).
- **CSRF token** only appears inside the rendered login HTML (hidden input
  `__anti-forgery-token`) — scrape it with regex, don't try to read it from
  session state directly. It can be sent back via the `x-csrf-token` header
  (avoids polluting the closed `LoginForm` schema with an extra form field).
- **Session cookie value doesn't change on successful login** — the store
  overwrites session *contents*, not the cookie key. A failed login (401),
  however, causes the session to be deleted server-side (no `:session` key
  in that handler's response), so a cookie captured before a failed attempt
  is no longer valid afterward.
- Full narrative of the above (why, not just what) is in
  `docs/login-integration-tests.md` — read it before touching login/CSRF
  integration tests again.

## Conventions

- Route handlers live under `src/microservice_boilerplate/ports/http_in/`,
  split by concern: `pages.clj` (server-rendered HTML) and `api.clj` (JSON).
  Routes mirror this split: `routes/pages.clj` and `routes/api.clj`, composed
  into the final route vector in `routes.clj`. Interceptors (CSRF, etc.) are
  attached per-group in the respective routes file. Templates rendered via
  `templates.clj` (selmer).
- Don't add error handling/validation beyond what schema already enforces at
  the boundary — this codebase leans on `prismatic/schema` for that.
- Open questions / follow-ups for this project go in `docs/TODO.md`, not
  inline TODO comments scattered in source.
- Whenever a new adapter fn is added (`src/microservice_boilerplate/adapters*`),
  write unit tests for it using `schema-generators.generators` (`g/generator`)
  driving `clojure.test.check.clojure-test/defspec` property tests, not just
  example-based `deftest`. Mirror `test/unit/microservice_boilerplate/adapters_test.clj`
  and `.../adapters/users_test.clj`: `(use-fixtures :once schema.test/validate-schemas)`,
  generate inputs with `g/generator <schema>` (pass `schemas.types/TypesLeafGenerators`
  as the leaf-generator override for constrained types like `PositiveNumber`/
  `NegativeNumber`), assert with `s/validate` on the output schema, and add a
  round-trip equality check (`model->db`∘`db->model` = identity) when the
  adapter is a pure key-mapping transform. Place the test file mirroring the
  source path under `test/unit/microservice_boilerplate/...`.
