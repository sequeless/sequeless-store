package org.sequeless.store.postgres;

import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The store's test-only schema setup (spec 18, G1 and Q-E). The Query migration set is not in this repository, so the
 * store's integration tests run the Command set and create {@code sq_searchable_property} themselves, at the hook between
 * Command versions 6 and 7 that {@link PostgresMigrations#migrate(DataSource, java.util.function.Consumer)} provides. The
 * DDL is a copy of the Query set's V1 table (its {@code frozen-migrations/post-p7/query} copy in the prototype) and creates
 * the table only if it is absent, so it is safe to run again. The host's drift test runs the real Query set instead.
 */
final class StoreTestSchema {

    /** Copied from the Query set's V1: the searchable-property registry that the Command V6 trigger reads. */
    static final String SEARCHABLE_PROPERTY_DDL = """
            CREATE TABLE IF NOT EXISTS sq_searchable_property (
                tenant_id     text NOT NULL,
                property_iri  text NOT NULL,
                PRIMARY KEY (tenant_id, property_iri)
            )
            """;

    private StoreTestSchema() {}

    /**
     * Migrates the Command set and creates {@code sq_searchable_property} between Command V6 and V7, so the store's
     * integration tests get the schema the trigger needs without the Query set.
     *
     * @param dataSource the data source to migrate; must not be {@code null}
     */
    static void migrate(DataSource dataSource) {
        PostgresMigrations.migrate(dataSource, StoreTestSchema::createSearchableProperty);
    }

    /**
     * Creates {@code sq_searchable_property} with the copied DDL, if it is absent.
     *
     * @param dataSource the data source; must not be {@code null}
     */
    static void createSearchableProperty(DataSource dataSource) {
        JdbcClient.create(dataSource).sql(SEARCHABLE_PROPERTY_DDL).update();
    }
}
