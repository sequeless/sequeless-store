# Provenance

This repository takes its Command side from `sequeless/sequeless-prototype`. The code was copied, not moved: the prototype
keeps its copies until its cut-over commit (spec 18 unit U11), which names this repository and the import SHA below.

| Field | Value |
|---|---|
| Source repository | `sequeless/sequeless-prototype` |
| Import SHA | `429b2de` (prototype `origin/main`, "Spec 18 U6 PR-2: delete the query-side testkit, add query-testkit to the host tests (Refs #46) (#232)") |
| Spec | `docs/extraction/spec-18-prototype-stage5-final-fold.md` in the prototype, sections 5 and 10 (`Refs sequeless/sequeless-prototype#46`) |
| Copy made | 2026-11-27, spec 18 unit U8, on branch `spec-18-u7-scaffold` (PR #2) |
| Merge date | Recorded when PR #2 merges. The merge is a later step, after the owner's SonarCloud import. |

## Source paths

The package names in the copy are the names the prototype had at the import SHA. The rename units U2 to U4 moved the
store packages before the copy, and the copy does not rename anything again.

| In this repository | Source at `429b2de` | Before the rename (prototype base `5c0fffa`) |
|---|---|---|
| `store-core/src/main/java/org/sequeless/store/spi` | `sequeless-spi/src/main/java/org/sequeless/store/spi` | `org.sequeless.spi.{object, outbox, authz, automation, derivation, validation, query}` (U2) |
| `store-core/src/main/java/org/sequeless/store/api` | `sequeless-spi/src/main/java/org/sequeless/store/api` | `org.sequeless.spi.{authz, automation}` (U3) |
| `store-core/src/main/java/org/sequeless/store/core` | `sequeless-core/src/main/java/org/sequeless/store/core` | `org.sequeless.core` (U4) |
| `store-core/src/main/java/org/sequeless/spi/object` | `sequeless-spi/src/main/java/org/sequeless/spi/object` | unchanged |
| `store-core/src/main/java/org/sequeless/spi/outbox` | `sequeless-spi/src/main/java/org/sequeless/spi/outbox` | unchanged |
| `store-testkit/src/main/java/org/sequeless/testkit` | `sequeless-spi-testkit/src/main/java/org/sequeless/testkit` (Command doubles, U5) | unchanged |
| `store-testkit/src/main/resources` | `sequeless-spi-testkit/src/main/resources` (`META-INF/services` and `ontology/unknown-plugin.ttl`) | unchanged |
| `store-postgres-jsonb-adapter/src/main/java/org/sequeless/store/postgres` | `sequeless-adapter-persistence-postgres/src/main/java/org/sequeless/store/postgres` (Command classes) | `org.sequeless.adapter.persistence.postgres` (U4) |
| `store-postgres-jsonb-adapter/src/main/resources/db/migration/sequeless/postgres` | `sequeless-adapter-persistence-postgres/src/main/resources/db/migration/sequeless/postgres` (V1, V3 to V12) | unchanged |
| `store-postgres-jsonb-adapter/src/test/resources/frozen-migrations/{post-p2,post-p3,post-p4,post-p7}/command` | the same paths in the prototype | unchanged |

Tests follow their sources into the matching `src/test` trees.

## Changes made in the copy

Everything in the copy is the import SHA's content, except the following. Each change is listed so that a diff against
`429b2de` shows nothing else.

1. **Javadoc-only imports.** `store.spi.Criterion`, `store.spi.AggregateRequest` and `store.spi.QueryPort` imported
   `org.sequeless.spi.query` types only for javadoc links. The query package stays in the host (spec Q18), so the
   imports are removed and the links are `{@code ...}`.
2. **Placeholders removed.** The scaffold's `BoundaryRulesTest` and `org/sequeless/store/testkit/package-info.java`
   are deleted. The scaffold's `package-info.java` files in `store.api`, `store.spi` and `store.core` are replaced by the
   copied ones.
3. **Store architecture rules (spec G3).** `store-core/src/test/java/org/sequeless/store/ArchitectureTest.java` is new.
   It carries the Command subset of the host's `ArchitectureTest` rules: SPI and API depend only on the JDK and the
   released contracts, core depends only on the allowed packages, and neither layer depends on Spring, Lombok or Jena,
   and core does not depend on JDBC or Temporal.
4. **Test-only schema (spec G1 and Q-E).** `store-postgres-jsonb-adapter/src/test/.../StoreTestSchema.java` is new. It
   runs the Command set through the hook between Command V6 and V7, and creates `sq_searchable_property` with a copy of
   the Query set's V1 table. Nine copied integration tests change one line each, from `PostgresMigrations.migrate(dataSource)`
   to `StoreTestSchema.migrate(dataSource)`: `PostgresAutomationFailureRecorderIT`, `PostgresObjectStoreContractIT`,
   `PostgresOntologyDocumentStoreContractIT`, `PostgresOutboxClaimIT`, `PostgresOutboxDecodeFailureIT`,
   `PostgresOutboxIndexV10IT`, `PostgresOutboxPortIT`, `PostgresQueryStoreAggregateContractIT` and
   `PostgresSearchVectorBackfillIT`.
5. **Settling test (spec G4).** `PostgresMigrationsSettlingIT` keeps the command-only cases: fresh database, frozen
   post-P7, post-P2 and post-P3 pairs, re-migrating an up-to-date database, and a command-only write that must fail.
   Its cut-over and P2a cases (frozen `3328993` and `fbb82c5`), its Query-history cases and its default-write cases
   stay in the host.
6. **Frozen-sets test.** `PostgresMigrationFrozenSetsTest` drops the Query-set cases. The Command-set checks are unchanged.
   `PostgresQueryStoreIndexUsageIT` drops `sq_index_registry` from its teardown `TRUNCATE`, because the store does not
   create that Query-set table. Its test body is unchanged.
7. **New build files.** The three module POMs, the aggregator's module list, the release-please entry for
   `store-postgres-jsonb-adapter` (initial version 0.1.0, skip-snapshot), and the Sonar coverage path for that module.

## Not carried, and known stale text

- **Host rule `noAdapterDependsOnCore` (adapters must not depend on core).** It covers `org.sequeless.store.postgres..`,
  but store-core's test cannot see the adapter classes, so the rule cannot run there. It is not carried in U8. The host's
  `ArchitectureTest` keeps the rule, and it applies to these classes once they are on the host's classpath (U11). Whether
  to add it to this repository is an open placement question for the Navigator, because spec G3 places the store rules in store-core.
- **Stale javadoc in the copy.** Some copied javadoc still names the prototype host's `sequeless-app` ArchUnit rules and
  says the `bannedDependencies` rule is in the module POM. The rule now comes from the parent POM through
  `sequeless.enforcer.core.skip`. The copy keeps these lines, as a pure copy must. A follow-up may correct them.
- **Classpath condition for the default migrate.** `PostgresMigrations.migrate(DataSource)` runs the Query set from the
  classpath. In this repository that path is exercised by nothing, because the tests use the command-only hook. The host
  must carry `query-postgres-jsonb-adapter` at runtime.
- **`junit-jupiter-api` is declared** in store-core and store-postgres at test scope, because `dependency:analyze` with
  `failOnWarning` reports it as used undeclared without the explicit entry.

## Host-only, not copied

These stay in the prototype host until the fold units (spec section 10):

- `PostgresPersistenceAutoConfiguration`, `PostgresPersistenceProperties`, `PostgresAdapterDescriptorProvider` and
  `PostgresMigratingDataSource`, and their Spring registration files (Q8, Q-A).
- The frozen directories `frozen-migrations/3328993` and `frozen-migrations/fbb82c5` (Q-B).
- The Query migration set and `frozen-migrations/post-p7/query` (retired from this repository, Q15 and Q23).
- `AdapterDescriptor`, `AdapterDescriptorProvider` and `SpiVersion` in the root `sequeless-spi` module (Q22).
- The Query and browse ports and their types under `org.sequeless.spi.query`.
