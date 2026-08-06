# Family Money engineering guide

This file defines repository-wide rules for humans and coding agents. Product
behaviour is specified in `docs/product/initial-requirements.md`. Accepted
architecture decisions are indexed in `docs/architecture/adr/README.md`.

If a requested change conflicts with an accepted ADR, do not work around the
decision silently. Explain the conflict and add a superseding ADR when the
architecture is intentionally changed.

## Architecture

- Keep the backend a single Gradle module and a modular monolith.
- Use the root package `io.familymoney`.
- Organize backend code by business module first: `portfolio`, `planning`,
  `reporting`, `integration`, `sharedkernel`, and `bootstrap`.
- Code outside a module may import only `<module>.api`; never import another
  module's `<module>.internal..`.
- Keep `sharedkernel` small and free of dependencies on business modules. Do not
  use it as a generic utilities package.
- Keep domain and application code independent of Kora HTTP types, generated API
  types, persistence records, Timefold types, and broker SDK types.
- Update ArchUnit tests whenever an allowed module dependency changes.

See `docs/architecture/adr/0001-modular-monolith.md`.

## Financial correctness

- Use `BigDecimal` in Kotlin and `Decimal` from `decimal.js` in TypeScript for
  all authoritative financial operations.
- Never use Kotlin/Java `float` or `double`, or JavaScript `number`, for money,
  prices, ratios, commissions, balances, or business decisions.
- Represent financial values in JSON as canonical decimal strings with a dot
  separator and no exponent.
- Keep `Money`, `Price`, `Ratio`, and `CommissionRate` as distinct semantic
  types.
- Round only in an explicitly named business operation with a documented rule.
  Preserve imported market-price precision.
- Revalidate every solver result with independent domain code before returning
  or persisting it.

## API workflow

- Treat `api/main.tsp` as the source of truth for the HTTP API.
- Do not hand-edit `api/generated/openapi.yaml`, generated Kotlin API types, or
  the generated TypeScript client.
- After a TypeSpec change, run `generateOpenApi` and commit the updated
  `api/generated/openapi.yaml`.
- Give every operation a stable `operationId` and keep object schemas closed to
  unknown fields.
- Generated Kotlin and TypeScript code belongs in build directories and must not
  be committed.

See `docs/architecture/adr/0003-contract-first-api.md`.

## Persistence

- Use PostgreSQL 17, synchronous JDBC, explicit SQL, Kora JDBC repositories, and
  Flyway.
- Keep production, local development, and Embedded PostgreSQL integration tests
  on the same PostgreSQL major.
- Store financial decimals as unconstrained PostgreSQL `numeric`; enforce
  semantic scale/range in domain types and safe `CHECK` constraints.
- Add every schema change as a forward Flyway migration. Never edit a migration
  that may already have been applied.
- Repository and transaction integration tests must run real Flyway migrations
  against Embedded PostgreSQL.
- Do not add SQLite-specific code or an ORM that hides generated SQL without a
  superseding decision.

## Frontend

- Use Vue 3, strict TypeScript, Vite, PrimeVue, TanStack Query, Zod, and
  `decimal.js`.
- Keep server state in TanStack Query. Add Pinia only for proven shared client
  state that does not fit query cache or local component state.
- Use a string-preserving `DecimalInput` for financial fields.
- Restrict conversion to JavaScript `number` to chart presentation adapters.
  Labels and tooltips must use the exact original decimal value.
- Give every canvas chart an accessible tabular representation.
- Prefer PrimeVue themes/design tokens and scoped CSS. Do not introduce Tailwind
  without an explicit decision.

## Solver

- Access Timefold only through the internal `TradePlanSolver` port.
- Keep hard financial constraints in both solver constraints and the independent
  post-solve validator.
- Preserve deterministic results with fixed seeds, ordered collections, and a
  step-based termination condition.
- Allow no more than one concurrent solve in the initial production
  configuration.
- Add or update versioned JSON fixtures and property-based tests when solver
  behaviour or score rules change.

## External integrations and secrets

- Manual prices, positions, and cash are always available even when integrations
  fail.
- T-Invest is the only planned MVP provider. Use read-only tokens only.
- Never implement order placement or accept full-access broker tokens for MVP.
- Show a diff and require confirmation before applying synchronized positions.
- Keep provider models behind `MarketPriceProvider` and
  `BrokerPortfolioProvider`.
- Never commit tokens, credentials, encryption keys, `.env` files containing
  secrets, production dumps, or decrypted financial data.

## Build and validation

Gradle is the canonical entry point. On Windows use `gradlew.bat`; on Unix use
`./gradlew`.

```text
./gradlew generateOpenApi
./gradlew check
./gradlew build
./gradlew spotlessApply
```

- Run `build` before declaring an implementation complete.
- Run focused tests during development, then the full relevant checks.
- Use KSP for Kora code generation in Kotlin. KAPT may be used only as a
  temporary diagnostic workaround; resolve KSP compatibility problems and
  restore KSP before declaring the build bootstrap complete.
- `check` must cover backend tests, ArchUnit, TypeSpec/OpenAPI drift, frontend
  typechecking, and frontend unit tests.
- Playwright runs as a separate end-to-end CI job using Chromium for MVP.
- Use Spotless/ktlint for mechanical Kotlin formatting and Detekt for semantic
  analysis. Keep Detekt formatting rules disabled.
- Use ESLint for Vue/TypeScript analysis and Prettier for frontend, TypeSpec,
  JSON, YAML, and Markdown formatting.
- Treat coverage reports as diagnostics, not as a substitute for meaningful
  invariant and scenario tests.
- Set explicit timeouts on agent tool calls: normally 10 seconds for file and
  diagnostic commands, up to 60 seconds for builds and tests, and 5 seconds for
  launching a background service. Split longer work into observable steps and
  report progress at least once per minute.
- Do not combine starting a persistent service and checking its health in one
  blocking command. Start it in a hidden background process, return promptly,
  and probe readiness with separate bounded commands.

The repository is initially documentation-only. When bootstrapping the build,
create these wrapper tasks and directory conventions before relying on ad-hoc
commands.

## Dependencies

- Centralize JVM dependency and plugin versions in the Gradle version catalog.
- Use Node.js 24 LTS and the npm version shipped with it.
- Manage JavaScript dependencies from the root npm workspace. Use exact direct
  dependency versions and commit the single root `package-lock.json`.
- Use `npm install` when intentionally changing dependencies and `npm ci` for
  reproducible installation.
- Do not auto-merge dependency updates. Major updates to Java, Kotlin, Kora,
  Gradle, TypeSpec, generators, or PostgreSQL require explicit review.

## Deployment safety

- Production runs on the Vultr VPS as separate application and PostgreSQL Docker
  Compose services.
- Do not remove or recreate the PostgreSQL volume as part of a routine deploy.
- Do not run destructive database commands, restore a dump, rotate production
  secrets, or deploy externally unless the user explicitly requests it.
- Keep the initial JVM, connection-pool, and PostgreSQL resource limits from
  `docs/architecture/adr/0008-deployment-and-backup.md`.
- A successful local dump is not sufficient: encrypted Backblaze B2 upload,
  retention, archive validation, and monthly restore testing are part of the
  backup definition of done.

## Definition of done

- The change follows product requirements and all active ADRs.
- Relevant unit, property, integration, architecture, contract, frontend, and
  end-to-end tests are updated.
- Generated OpenAPI is current; other generated code is not committed.
- Formatting, static analysis, and `build` pass.
- Documentation and ADRs are updated when behaviour or architecture changes.
- No secrets, production data, IDE metadata, or unrelated local changes are
  included.
