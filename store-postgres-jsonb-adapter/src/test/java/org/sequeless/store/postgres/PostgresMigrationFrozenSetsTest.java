package org.sequeless.store.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Proves the frozen post-P7, post-P2, post-P3 and post-P4 Command migration sets in test resources still match the live
 * sets. The frozen command sets are subsets of the live command set: later migrations may be added to the live set after
 * the freeze, as V7 was. Every frozen file is byte-identical to its live file. P2's PR copies its own migration pair from
 * the post-P7 set, so a silent drift between the two would corrupt that copy. Each of the post-P2, post-P3 and post-P4
 * sets is a saved copy of the live set after its step, and the settling pairs run from one to the next, so they are
 * checked the same way.
 */
class PostgresMigrationFrozenSetsTest {

    private static final String LIVE_COMMAND = "db/migration/sequeless/postgres";
    private static final String FROZEN_COMMAND = "frozen-migrations/post-p7/command";
    private static final String FROZEN_POST_P2_COMMAND = "frozen-migrations/post-p2/command";
    private static final String FROZEN_POST_P3_COMMAND = "frozen-migrations/post-p3/command";
    private static final String FROZEN_POST_P4_COMMAND = "frozen-migrations/post-p4/command";

    /** The live command set is V1 and V3 to V12. There is no V2: the V2 deleted at 859a118 was the query set. */
    private static final List<Integer> LIVE_COMMAND_VERSIONS = List.of(1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

    @Test
    void liveCommandSetContainsV1AndV3ToV12AndNoV2() {
        List<String> names = sqlFileNames(LIVE_COMMAND);
        assertThat(names).allMatch(name -> name.matches("V\\d+__.*\\.sql"));
        assertThat(names.stream()
                        .map(PostgresMigrationFrozenSetsTest::versionOf)
                        .toList())
                .containsAll(LIVE_COMMAND_VERSIONS)
                .doesNotContain(2);
    }

    @Test
    void frozenCommandSetIsASubsetOfTheLiveCommandSet() {
        assertThat(sqlFileNames(LIVE_COMMAND)).containsAll(sqlFileNames(FROZEN_COMMAND));
    }

    @Test
    void everyFrozenPostP7FileIsByteIdenticalToItsLiveFile() {
        assertIdentical(FROZEN_COMMAND, LIVE_COMMAND);
    }

    @Test
    void frozenPostP2CommandSetIsASubsetOfTheLiveCommandSet() {
        assertThat(sqlFileNames(LIVE_COMMAND)).containsAll(sqlFileNames(FROZEN_POST_P2_COMMAND));
    }

    @Test
    void everyFrozenPostP2FileIsByteIdenticalToItsLiveFile() {
        assertIdentical(FROZEN_POST_P2_COMMAND, LIVE_COMMAND);
    }

    @Test
    void frozenPostP3CommandSetIsASubsetOfTheLiveCommandSet() {
        assertThat(sqlFileNames(LIVE_COMMAND)).containsAll(sqlFileNames(FROZEN_POST_P3_COMMAND));
    }

    @Test
    void everyFrozenPostP3FileIsByteIdenticalToItsLiveFile() {
        assertIdentical(FROZEN_POST_P3_COMMAND, LIVE_COMMAND);
    }

    @Test
    void frozenPostP4CommandSetIsASubsetOfTheLiveCommandSet() {
        assertThat(sqlFileNames(LIVE_COMMAND)).containsAll(sqlFileNames(FROZEN_POST_P4_COMMAND));
    }

    @Test
    void everyFrozenPostP4FileIsByteIdenticalToItsLiveFile() {
        assertIdentical(FROZEN_POST_P4_COMMAND, LIVE_COMMAND);
    }

    /**
     * Pairs each frozen file with the live file of the same name and compares bytes. Identity is the file name and
     * the content, never the directory the frozen copy sits in, so moving a frozen set does not change the check.
     */
    private static void assertIdentical(String frozenDirectory, String liveDirectory) {
        for (String name : sqlFileNames(frozenDirectory)) {
            assertThat(readResource(frozenDirectory + "/" + name))
                    .as("%s/%s must match the live file byte for byte", frozenDirectory, name)
                    .isEqualTo(readResource(liveDirectory + "/" + name));
        }
    }

    private static int versionOf(String fileName) {
        return Integer.parseInt(fileName.substring(1, fileName.indexOf("__")));
    }

    private static List<String> sqlFileNames(String directory) {
        URL url = PostgresMigrationFrozenSetsTest.class.getClassLoader().getResource(directory);
        assertThat(url).as("classpath directory %s", directory).isNotNull();
        try (Stream<Path> files = Files.list(Path.of(url.toURI()))) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".sql"))
                    .sorted()
                    .toList();
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("cannot list " + directory, e);
        }
    }

    private static byte[] readResource(String resource) {
        try (InputStream in =
                PostgresMigrationFrozenSetsTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("classpath resource %s", resource).isNotNull();
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + resource, e);
        }
    }
}
