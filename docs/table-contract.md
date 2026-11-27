# Table contract

This repository owns the Command-side tables and their migration history. Other repositories, such as
`sequeless-ontology`, read these tables and rely on their names and shapes. Changing a name or a column here is a
breaking change for them.

## Tables

| Table | Created by | Read by |
|---|---|---|
| `sq_object` | Command V1 | the Postgres adapter, the query adapter, and the search trigger (V6) |
| `sq_ontology_document` | Command V1 | `ontology-postgres-adapter` (spec 18 U13), which reads it and ships no migration of its own |
| `sq_outbox` | Command V1 (later columns and indexes in V3 to V11) | the outbox relay |
| `sq_automation_failure` | Command V12 | the automation failure recorder |

`sq_object` also has a search-vector column and a trigger, both added in V6, and a GIN index on that column. The GIN index
is not in this repository: it is part of the Query set in the host.

## Migration history

- The Command set is V1 and V3 to V12. There is no V2. The V2 that existed before the Query split was deleted at
  `859a118` in the prototype, and it was the Query set's file, not a Command file. Do not renumber, and do not add a V2.
- The history table is Flyway's default, `flyway_schema_history`. The Query set uses its own history table in the host.
- V1 to V12 are byte-identical to the prototype's files at the import SHA `429b2de` (see `PROVENANCE.md`). The frozen
  copies under `src/test/resources/frozen-migrations/{post-p2,post-p3,post-p4,post-p7}/command` must match the live files
  byte for byte. `PostgresMigrationFrozenSetsTest` checks both rules.
- `*:missing` and `*:future` are ignored, and the transactional advisory lock is off (`flyway.postgresql.transactional.lock`
  set to `false`). Both settings are required: a database that applied V2 before the split keeps a `MISSING_SUCCESS` row,
  and V10 uses `CREATE INDEX CONCURRENTLY`, which waits on an open transaction.

## The V6 coupling

Command V6 creates a trigger on `sq_object` that reads `sq_searchable_property`. That table is created by the Query set,
which is not in this repository. The consequences:

- A bare database that runs only the Command set and then writes an object fails on V6's trigger, with
  `relation "sq_searchable_property" does not exist`.
- `PostgresMigrations.migrate(DataSource, Consumer<DataSource>)` runs the hook between Command V6 and V7. The default
  `migrate(DataSource)` runs the Query set in that hook. That works only when the Query set is on the runtime classpath, as
  it is in the host, which carries `query-postgres-jsonb-adapter`. Without it the default `migrate` finds no Query location,
  applies the Command set alone, and the first object write fails. A caller can pass a no-op hook for the Command set alone.
- This repository's integration tests create `sq_searchable_property` with a copy of the Query set's V1 table, in the
  hook (`StoreTestSchema`). The host's drift test runs the real Query set instead.

If the Query set changes `sq_searchable_property`, the copy in `StoreTestSchema` must change with it.
