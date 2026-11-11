package be.achzak.civicgrant;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import be.achzak.civicgrant.domain.audit.service.RecordAuditEntryService;
import be.achzak.civicgrant.domain.shared.Actor;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

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
    static final ArchRule every_use_case_checks_the_actors_role = methods()
            .that()
            .arePublic()
            .and()
            .areDeclaredInClassesThat()
            .resideInAPackage("..civicgrant.domain..service..")
            .and()
            .areNotDeclaredIn(RecordAuditEntryService.class)
            .should(callActorRequireAnyRole());

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

    private static ArchCondition<JavaMethod> callActorRequireAnyRole() {
        return new ArchCondition<>("call Actor.requireAnyRole") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean checksRole = method.getMethodCallsFromSelf().stream()
                        .map(JavaMethodCall::getTarget)
                        .anyMatch(target -> target.getOwner().isEquivalentTo(Actor.class)
                                && target.getName().equals("requireAnyRole"));
                events.add(new SimpleConditionEvent(
                        method,
                        checksRole,
                        method.getFullName() + (checksRole ? " calls" : " doesn't call") + " Actor.requireAnyRole"));
            }
        };
    }
}
