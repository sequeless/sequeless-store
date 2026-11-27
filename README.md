# sequeless-store

The command side store of a Sequeless host: object state, outbox, validation, derivation and the
Postgres adapter for them. It is published from this repository, and the prototype consumes it
through the Sequeless BOM.

Modules:

- `store-core`: the store's ports, value types and Command use cases. No Spring, no Postgres.
- `store-testkit`: in-memory doubles, the reference-domain fixtures and the contract tests for the store. No Spring.
- `store-postgres-jsonb-adapter`: the Command-side Postgres adapter and the Command migrations V1 to V12, which are the
  published table contract (see `docs/table-contract.md`). Its integration tests need Docker for Testcontainers.

`PROVENANCE.md` records the copy from `sequeless-prototype` (import SHA `429b2de`).

The extraction plan is in `docs/extraction` in the prototype repo.

## Build

```bash
make verify        # or: ./mvnw -B verify
```

The parent POM (`sequeless-build-parent` 0.1.3) and the reusable workflows (`workflows-v0.1.3`)
come from released `sequeless-build` versions, and CI runs against them. Renovate moves both.

## Local setup

The parent POM comes from GitHub Packages, so export a token with `read:packages` as
`GITHUB_PACKAGES_TOKEN` before any `mvn` run. The `github` server in `~/.m2/settings.xml` reads it
as `<password>${env.GITHUB_PACKAGES_TOKEN}</password>`; `sequeless-build`'s README has the full entry.

If `spotless:check` fails at `verify`, run `mvn spotless:apply`, then `mvn verify` again. The parent
rejects a POM that is valid XML but not in the formatter's layout.

## Release

release-please runs in manifest mode with one config for the store modules, first version 0.1.0.
The release PRs are release-please PRs in this repository.
