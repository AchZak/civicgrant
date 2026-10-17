package be.achzak.civicgrant;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "be.achzak.civicgrant", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses()
            .that()
            .resideInAPackage("..civicgrant.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "..civicgrant.api..",
                    "..civicgrant.persistence..")
            .allowEmptyShould(true);

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
}
