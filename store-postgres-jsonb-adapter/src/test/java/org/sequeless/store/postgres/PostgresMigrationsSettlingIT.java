package org.sequeless.store.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Settles the Command migration set of spec 13, P7 (spec 13 lines 121 and 128), against a real PostgreSQL 18
 * started by Testcontainers. Each test empties the {@code public} schema first, so it starts with no migration
 * history.
 *
 * <p>This is the store's command-only settling test (spec 18, G4). The Query set is not in this repository, so
 * {@code sq_searchable_property} is created with the copied DDL (G1) and the Query history is not checked here.
 * The cut-over and P2a starting states (frozen {@code 3328993} and {@code fbb82c5}) and the Query-history cases stay
 * in the host, which owns those frozen directories (Q-B, Q23).
 *
 * <p>The starting states are: a fresh database, and the frozen post-P7, post-P2 and post-P3 sets. The pair from
 * after P7 to after P2 (case f) starts at the frozen post-P7 set, migrates to the frozen post-P2 set, and then to the
 * live set. The pair from after P2 to after P3 (case g) starts at the frozen post-P2 set, migrates to the frozen
 * post-P3 set, and then to the live set. The pair from after P3 to after P4 (case h) starts at the frozen post-P3
 * set, migrates to the frozen post-P4 set, and then to the live set.
 */
@Testcontainers
class PostgresMigrationsSettlingIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    private static final String FROZEN_POST_P7 = "classpath:frozen-migrations/post-p7/command";
    private static final String FROZEN_POST_P2 = "classpath:frozen-migrations/post-p2/command";
    private static final String FROZEN_POST_P3 = "classpath:frozen-migrations/post-p3/command";
    private static final String FROZEN_POST_P4 = "classpath:frozen-migrations/post-p4/command";
    private static final String TENANT = "acme";
    private static final String TITLE_IRI = "https://sequeless.test/settling#title";

    /** The Command history of the final set, with no entry for V2, which the final set no longer has. */
    private static final List<String> FINAL_COMMAND_HISTORY = List.of(
            "1 SUCCESS",
            "3 SUCCESS",
            "4 SUCCESS",
            "5 SUCCESS",
            "6 SUCCESS",
            "7 SUCCESS",
            "8 SUCCESS",
            "9 SUCCESS",
            "10 SUCCESS",
            "11 SUCCESS",
            "12 SUCCESS");

    private static DataSource dataSource;

    @BeforeAll
    static void createDataSource() {
        dataSource = new SimpleDriverDataSource(
                new org.postgresql.Driver(), POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @BeforeEach
    void emptyDatabase() {
        JdbcClient.create(dataSource).sql("DROP SCHEMA public CASCADE").update();
        JdbcClient.create(dataSource).sql("CREATE SCHEMA public").update();
    }

    /**
     * Settling case (f): the pair from after P7 to after P2. A database at the frozen post-P7 set (V1 and V3
     * to V6, no V2) migrates to the frozen post-P2 set (V1 and V3 to V8), and then to the live set.
     */
    @Test
    void frozenPostP7LayoutMigratesThroughThePostP2Set() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations(FROZEN_POST_P7)
                .load()
                .migrate();
        // A database at the post-P7 set has also run the Query set, which creates sq_searchable_property;
        // V6's trigger reads that table on every object write.
        StoreTestSchema.createSearchableProperty(dataSource);
        insertObject(UUID.randomUUID(), "first");
        insertObject(UUID.randomUUID(), "second");
        insertSearchableRegistration();
        Flyway.configure()
                .dataSource(dataSource)
                .locations(FROZEN_POST_P2)
                .load()
                .migrate();

        StoreTestSchema.migrate(dataSource);

        assertThat(versionsAndStates(PostgresMigrations.commandFlyway(dataSource)))
                .containsExactlyElementsOf(FINAL_COMMAND_HISTORY);
        assertSearchSchemaPresent();
        assertRowCounts(2, 1);
    }

    /**
     * Settling case (g): the pair from after P2 to after P3. A database at the frozen post-P2 set (V1 and V3
     * to V8, no V2) and the Query set migrates to the frozen post-P3 set (V1 and V3 to V9), and then to the
     * live set.
     */
    @Test
    void frozenPostP2LayoutMigratesThroughThePostP3Set() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations(FROZEN_POST_P2)
                .load()
                .migrate();
        // The post-P2 set has the search trigger, which reads sq_searchable_property on every object write.
        StoreTestSchema.createSearchableProperty(dataSource);
        insertObject(UUID.randomUUID(), "first");
        insertObject(UUID.randomUUID(), "second");
        insertSearchableRegistration();
        Flyway.configure()
                .dataSource(dataSource)
                .locations(FROZEN_POST_P3)
                .load()
                .migrate();

        StoreTestSchema.migrate(dataSource);

        assertThat(versionsAndStates(PostgresMigrations.commandFlyway(dataSource)))
                .containsExactlyElementsOf(FINAL_COMMAND_HISTORY);
        assertSearchSchemaPresent();
        assertRowCounts(2, 1);
    }

    /**
     * Settling case (h): the pair from after P3 to after P4. A database at the frozen post-P3 set (V1 and V3
     * to V9, no V10 or V11) and the Query set migrates to the frozen post-P4 set (V1 and V3 to V11), and then
     * to the live set.
     */
    @Test
    void frozenPostP3LayoutMigratesThroughThePostP4Set() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations(FROZEN_POST_P3)
                .load()
                .migrate();
        // The post-P3 set has the search trigger, which reads sq_searchable_property on every object write.
        StoreTestSchema.createSearchableProperty(dataSource);
        insertObject(UUID.randomUUID(), "first");
        insertObject(UUID.randomUUID(), "second");
        insertSearchableRegistration();
        // The post-P4 set ends with V10, CREATE INDEX CONCURRENTLY. With the transactional lock on, Flyway holds
        // a transaction open for the whole run and V10 waits on it forever, so the lock is off here, as in
        // PostgresMigrations.
        Flyway.configure()
                .dataSource(dataSource)
                .locations(FROZEN_POST_P4)
                .configuration(Map.of("flyway.postgresql.transactional.lock", "false"))
                .load()
                .migrate();

        StoreTestSchema.migrate(dataSource);

        assertThat(versionsAndStates(PostgresMigrations.commandFlyway(dataSource)))
                .containsExactlyElementsOf(FINAL_COMMAND_HISTORY);
        assertSearchSchemaPresent();
        assertRowCounts(2, 1);
    }

    /** Settling case (a): a fresh database migrates to the final set. */
    @Test
    void freshDatabaseMigratesToTheFinalSet() {
        StoreTestSchema.migrate(dataSource);

        assertThat(versionsAndStates(PostgresMigrations.commandFlyway(dataSource)))
                .containsExactlyElementsOf(FINAL_COMMAND_HISTORY);
        assertSearchSchemaPresent();

        insertObject(UUID.randomUUID(), "first");
        insertObject(UUID.randomUUID(), "second");
        insertSearchableRegistration();
        assertRowCounts(2, 1);
    }

    /** Settling case (e): migrating an up-to-date database again does not change the Command history. */
    @Test
    void migratingAnUpToDateDatabaseAgainChangesNoCommandHistory() {
        StoreTestSchema.migrate(dataSource);
        List<String> commandBefore = describedHistory(PostgresMigrations.commandFlyway(dataSource));

        StoreTestSchema.migrate(dataSource);

        assertThat(describedHistory(PostgresMigrations.commandFlyway(dataSource)))
                .isEqualTo(commandBefore);
    }

    /**
     * Bare database, command-only migrate (a no-op hook): the Query set is not applied, so the search trigger
     * reads a table that does not exist and an object write fails. This is the negative check for the command-only hook.
     */
    @Test
    void commandOnlyMigrateFailsAnObjectWriteBecauseNoQuerySetExists() {
        PostgresMigrations.migrate(dataSource, ignored -> {});

        // Spring wraps the driver error, so its message is the SQL text. The missing relation is in the cause.
        assertThatThrownBy(() -> insertObject(UUID.randomUUID(), "bare"))
                .hasStackTraceContaining("relation \"sq_searchable_property\" does not exist");
    }

    private static void assertSearchSchemaPresent() {
        Long triggers = JdbcClient.create(dataSource)
                .sql("SELECT COUNT(*) FROM pg_trigger WHERE tgrelid = 'sq_object'::regclass "
                        + "AND tgname = 'sq_object_search_vector_trigger' AND NOT tgisinternal")
                .query(Long.class)
                .single();
        assertThat(triggers).as("search trigger on sq_object").isEqualTo(1L);
    }

    private static void assertRowCounts(long objects, long registrations) {
        Long objectRows = JdbcClient.create(dataSource)
                .sql("SELECT COUNT(*) FROM sq_object")
                .query(Long.class)
                .single();
        Long registrationRows = JdbcClient.create(dataSource)
                .sql("SELECT COUNT(*) FROM sq_searchable_property")
                .query(Long.class)
                .single();
        assertThat(objectRows).as("sq_object rows").isEqualTo(objects);
        assertThat(registrationRows).as("sq_searchable_property rows").isEqualTo(registrations);
    }

    private static List<String> versionsAndStates(Flyway flyway) {
        return Arrays.stream(flyway.info().all())
                .map(info -> info.getVersion() + " " + info.getState())
                .toList();
    }

    private static List<String> describedHistory(Flyway flyway) {
        return Arrays.stream(flyway.info().all())
                .map(info -> describe(info) + " rank " + info.getInstalledRank())
                .toList();
    }

    private static String describe(MigrationInfo info) {
        return info.getVersion() + " " + info.getState();
    }

    private static void insertObject(UUID id, String title) {
        JdbcClient.create(dataSource)
                .sql("INSERT INTO sq_object (id, tenant_id, type_iri, version, props, created_at, created_by, "
                        + "updated_at, updated_by) VALUES (:id, :tenantId, :typeIri, 1, CAST(:props AS jsonb), "
                        + "now(), 'settling', now(), 'settling')")
                .param("id", id)
                .param("tenantId", TENANT)
                .param("typeIri", "https://sequeless.test/settling#Document")
                .param("props", "{\"" + TITLE_IRI + "\": {\"text\": \"" + title + "\"}}")
                .update();
    }

    private static void insertSearchableRegistration() {
        JdbcClient.create(dataSource)
                .sql("INSERT INTO sq_searchable_property (tenant_id, property_iri) VALUES (:tenantId, :propertyIri)")
                .param("tenantId", TENANT)
                .param("propertyIri", TITLE_IRI)
                .update();
    }
}
