package org.sequeless.store;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;

/**
 * The store's architecture rules, carried from the prototype's host {@code ArchitectureTest} (spec 18 G3). They
 * cover the Command side only: the ports and value types ({@code org.sequeless.spi.object}, {@code
 * org.sequeless.spi.outbox} and {@code org.sequeless.store.spi}), the inbound API ({@code org.sequeless.store.api})
 * and the use cases ({@code org.sequeless.store.core}). The host-only rules stay in the host's test.
 */
@AnalyzeClasses(
        packages = "org.sequeless",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureTest {

    private static final String SPI_PACKAGE = "org.sequeless.spi..";
    private static final String STORE_SPI_PACKAGE = "org.sequeless.store.spi..";
    private static final String STORE_API_PACKAGE = "org.sequeless.store.api..";
    private static final String CORE_PACKAGE = "org.sequeless.store.core..";
    private static final String KERNEL_PACKAGE = "org.sequeless.kernel..";
    private static final String EXPRESSION_PACKAGE = "org.sequeless.expression..";
    private static final String BEHAVIOUR_PACKAGE = "org.sequeless.ontology.behaviour..";
    private static final String AUTOMATION_PACKAGE = "org.sequeless.ontology.automation..";
    private static final String ONTOLOGY_PACKAGE = "org.sequeless.ontology.api..";
    private static final String ONTOLOGY_SPI_PACKAGE = "org.sequeless.ontology.spi..";
    private static final String ONTOLOGY_SCHEMA_PACKAGE = "org.sequeless.ontology.schema..";
    private static final String SLF4J_PACKAGE = "org.slf4j";

    /**
     * The aggregate query types that spec 18 U2 moved from {@code org.sequeless.spi.query} into {@code org.sequeless.store.spi}.
     * {@link #querySeesOnlySchema} covers them here, as the host rule does for the query package.
     */
    private static final Set<String> MOVED_QUERY_TYPE_NAMES = Set.of(
            "org.sequeless.store.spi.QueryPort",
            "org.sequeless.store.spi.Criterion",
            "org.sequeless.store.spi.Operator",
            "org.sequeless.store.spi.AggregateRequest",
            "org.sequeless.store.spi.AggregateResult");

    private static final DescribedPredicate<JavaClass> MOVED_QUERY_TYPES = DescribedPredicate.describe(
            "is a moved aggregate query type (spec 18 U2)",
            javaClass -> MOVED_QUERY_TYPE_NAMES.contains(javaClass.getName()));

    private static final DescribedPredicate<JavaClass> PRIMITIVE_OR_ARRAY = DescribedPredicate.describe(
            "is a primitive or array type (ArchUnit places these in the default package)",
            javaClass -> javaClass.isPrimitive() || javaClass.isArray());

    @ArchTest
    static final ArchRule spiDependsOnlyOnJdkAndItself = classes()
            .that()
            .resideInAnyPackage(SPI_PACKAGE, STORE_SPI_PACKAGE, STORE_API_PACKAGE)
            .should()
            .onlyDependOnClassesThat(resideInAnyPackage(
                            "java..",
                            SPI_PACKAGE,
                            STORE_SPI_PACKAGE,
                            STORE_API_PACKAGE,
                            KERNEL_PACKAGE,
                            ONTOLOGY_PACKAGE,
                            ONTOLOGY_SPI_PACKAGE,
                            ONTOLOGY_SCHEMA_PACKAGE,
                            BEHAVIOUR_PACKAGE,
                            AUTOMATION_PACKAGE)
                    .or(PRIMITIVE_OR_ARRAY))
            .because("the store's ports are the outermost layer of the hexagon: they must be usable by any adapter "
                    + "or client without pulling in anything beyond the JDK, the released sequeless-kernel types, "
                    + "the released ontology contracts they name (ontology-core, schema-contract, behaviour-contract "
                    + "and automation-contract) and their own port types.");

    @ArchTest
    static final ArchRule coreDependsOnlyOnJdkSpiAndItself = classes()
            .that()
            .resideInAPackage(CORE_PACKAGE)
            .should()
            .onlyDependOnClassesThat(resideInAnyPackage(
                            "java..",
                            CORE_PACKAGE,
                            SPI_PACKAGE,
                            STORE_SPI_PACKAGE,
                            STORE_API_PACKAGE,
                            KERNEL_PACKAGE,
                            EXPRESSION_PACKAGE,
                            SLF4J_PACKAGE,
                            "org.sequeless.filter.api..",
                            "com.fasterxml.jackson.databind..",
                            ONTOLOGY_PACKAGE,
                            ONTOLOGY_SPI_PACKAGE,
                            ONTOLOGY_SCHEMA_PACKAGE,
                            BEHAVIOUR_PACKAGE,
                            AUTOMATION_PACKAGE)
                    .or(PRIMITIVE_OR_ARRAY))
            .because("the store's use cases hold the orchestration logic; they may depend on the JDK, the ports they "
                    + "call through, the released kernel identity and values types, the released ontology contracts, "
                    + "the expression api and the SLF4J facade, but nothing else, or the core would stop being portable "
                    + "across adapter choices. The filter api and Jackson 2's databind package are allowed only because "
                    + "the criteria bridge (FilterNodeToCriterion) reads a rollup's FieldFilter, whose value is a "
                    + "Jackson 2 JsonNode.");

    @ArchTest
    static final ArchRule spiAndCoreDoNotDependOnSpring = noClasses()
            .that()
            .resideInAnyPackage(SPI_PACKAGE, STORE_SPI_PACKAGE, STORE_API_PACKAGE, CORE_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .because("the store's ports and use cases are framework-agnostic: Spring is a wiring concern that belongs "
                    + "to the host and to the adapters' auto-configurations, never to the ports or use cases, or the "
                    + "hexagon could not be run or tested outside a Spring context.");

    @ArchTest
    static final ArchRule noLombokInSpiOrCore = noClasses()
            .that()
            .resideInAnyPackage(SPI_PACKAGE, STORE_SPI_PACKAGE, STORE_API_PACKAGE, CORE_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("lombok..")
            .because("the store's ports and use cases are plain, Lombok-free Java by design (no Lombok compiler plugin "
                    + "is configured for store-core); adapters may use Lombok, but the hexagon's core layers may not "
                    + "depend on it.");

    @ArchTest
    static final ArchRule coreDoesNotDependOnAdapterSpecificLibraries = noClasses()
            .that()
            .resideInAPackage(CORE_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.apache.jena..", "java.sql..", "javax.sql..", "io.temporal..")
            .because(
                    "the store's use cases must stay ignorant of which concrete technology backs each port at runtime; "
                            + "referencing Jena, JDBC or Temporal types directly would leak adapter implementation details into "
                            + "the use cases the ports exist to hide.");

    @ArchTest
    static final ArchRule noJenaInSpiOrCore = noClasses()
            .that()
            .resideInAnyPackage(SPI_PACKAGE, STORE_SPI_PACKAGE, STORE_API_PACKAGE, CORE_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.apache.jena..")
            .because("org.apache.jena is an adapter's own implementation library, not part of the framework-agnostic "
                    + "hexagon; a leaked Jena type would force every adapter of every port to drag Jena onto its "
                    + "classpath.");

    @ArchTest
    static final ArchRule querySeesOnlySchema = noClasses()
            .that(MOVED_QUERY_TYPES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(BEHAVIOUR_PACKAGE, AUTOMATION_PACKAGE, ONTOLOGY_PACKAGE)
            .because("the query port runs against one Schema and nothing else: it must not see the Behaviour or "
                    + "Automation halves, or the ontology administration port that activates them. The Schema is the "
                    + "only part of the type system a query needs.");
}
