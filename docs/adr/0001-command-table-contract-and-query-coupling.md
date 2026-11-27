# 0001. The Command table contract, and the V6 coupling to the Query set

- Status: accepted
- Date: 2026-11-27

## Context

Spec 18 of the prototype (`sequeless/sequeless-prototype#46`) moves the Command side into this repository. Other
repositories read the Command tables: `sequeless-ontology` reads `sq_ontology_document` (spec 18 U13), and the prototype
host reads `sq_object` through the query adapter. The Command set and the Query set share one database, and the Command
migration V6 needs a table that only the Query set creates. The two sets were split in spec 13 (P7). The split left two
facts that a reader of this repository cannot see from the Command files alone:

- Command V6 creates a trigger on `sq_object` that reads `sq_searchable_property`. A bare database that runs only the
  Command set fails at V6's trigger on the first object write.
- There is no Command V2. The V2 that the prototype deleted at `859a118` was the Query set's file. A database that applied
  V2 before the split keeps a `MISSING_SUCCESS` row for it.

## Decision

1. This repository owns the Command migrations V1 and V3 to V12, on Flyway's default history table. We will not renumber
   them and we will not add a V2. The V1 file creates `sq_object`, `sq_ontology_document` and `sq_outbox` together, as the
   published table contract. The table list and the migration rules are in `docs/table-contract.md`.
2. The V6 coupling is handled by a hook. `PostgresMigrations.migrate(DataSource, Consumer<DataSource>)` runs the hook
   between V6 and V7. The default `migrate(DataSource)` runs the Query set in that hook, so every existing caller keeps its
   behaviour, provided the Query set is on the runtime classpath (the host carries `query-postgres-jsonb-adapter`). A caller
   that passes a no-op hook gets the Command set alone.
3. The store's own integration tests run the Command set only. They create `sq_searchable_property` with a copy of the
   Query set's V1 table, in the hook. They do not depend on a Query artifact, at runtime or at test scope. The host's
   drift test runs the real Query set and then the Command set, so a change to the Query set's table is caught there.
4. The frozen Command sets (`post-p2`, `post-p3`, `post-p4` and `post-p7`) are copies of the live files, and the frozen-sets
   test pins them byte for byte. The cut-over and P2a frozen directories stay in the host, which owns them.

## Consequences

- A consumer that writes `sq_object` rows must run the Query set before the write, either by calling the default
  `migrate(DataSource)` with the Query set on its runtime classpath, or by running the Query set in its own hook. A consumer that calls `migrate` with a no-op hook and
  then writes gets `relation "sq_searchable_property" does not exist`. The store's settling test pins that behaviour.
- The copy of the Query set's table in `StoreTestSchema` must change when the Query set's table changes. The host's drift
  test is the check that catches a missed change.
- Removing the coupling would take a new migration after V12 that replaces the search trigger's function. V6 itself
  stays frozen, because the frozen-sets test pins its bytes.
