package be.achzak.civicgrant;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "be.achzak.civicgrant", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_depends_only_on_the_jdk = classes()
            .that()
            .resideInAPackage("..civicgrant.domain..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "..civicgrant.domain..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule shared_kernel_depends_on_no_aggregate = classes()
            .that()
            .resideInAPackage("..civicgrant.domain.shared..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "..civicgrant.domain.shared..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule no_cycles_between_aggregate_packages =
            slices().matching("..civicgrant.domain.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule nothing_else_in_the_domain_depends_on_audit = noClasses()
            .that()
            .resideInAPackage("..civicgrant.domain..")
            .and()
            .resideOutsideOfPackage("..civicgrant.domain.audit..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..civicgrant.domain.audit..");

    @ArchTest
    static final ArchRule api_does_not_use_persistence = noClasses()
            .that()
            .resideInAPackage("..civicgrant.api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..civicgrant.persistence..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistence_does_not_use_api = noClasses()
            .that()
            .resideInAPackage("..civicgrant.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..civicgrant.api..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule only_api_security_reads_the_security_context = noClasses()
            .that()
            .resideOutsideOfPackage("..civicgrant.api.security..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework.security.core.context..");
}
