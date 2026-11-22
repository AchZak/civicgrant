package be.achzak.civicgrant;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import be.achzak.civicgrant.domain.audit.service.RecordAuditEntryService;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Command;
import be.achzak.civicgrant.domain.shared.Query;
import be.achzak.civicgrant.persistence.documentstorage.DocumentBlobNames;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.util.List;

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
    static final ArchRule every_use_case_takes_exactly_one_command_or_query = methods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("..civicgrant.domain..port.in..")
            .and()
            .areDeclaredInClassesThat()
            .areInterfaces()
            .should(takeExactlyOneCommandOrQuery());

    @ArchTest
    static final ArchRule use_cases_are_interfaces_with_one_method = classes()
            .that()
            .resideInAPackage("..civicgrant.domain..port.in..")
            .and()
            .areInterfaces()
            .should()
            .haveSimpleNameEndingWith("UseCase")
            .andShould(declareExactlyOneMethod());

    @ArchTest
    static final ArchRule services_implement_exactly_one_use_case = classes()
            .that()
            .resideInAPackage("..civicgrant.domain..service..")
            .should()
            .haveSimpleNameEndingWith("Service")
            .andShould(implementExactlyOneUseCase());

    @ArchTest
    static final ArchRule commands_and_queries_are_records_next_to_their_use_case = classes()
            .that()
            .areAssignableTo(Command.class)
            .or()
            .areAssignableTo(Query.class)
            .and()
            .areNotInterfaces()
            .should()
            .beAssignableTo(Record.class)
            .andShould()
            .resideInAPackage("..civicgrant.domain..port.in..");

    @ArchTest
    static final ArchRule outside_the_domain_aggregates_are_only_read_through_accessors = noClasses()
            .that()
            .resideOutsideOfPackage("..civicgrant.domain..")
            .should()
            .callMethodWhere(aggregateBehaviour());

    @ArchTest
    static final ArchRule only_persistence_restores_aggregates = noClasses()
            .that()
            .resideOutsideOfPackage("..civicgrant.persistence..")
            .should()
            .callMethodWhere(aggregateRestore());

    @ArchTest
    static final ArchRule api_reaches_the_domain_through_use_cases_only = noClasses()
            .that()
            .resideInAPackage("..civicgrant.api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..civicgrant.domain..service..", "..civicgrant.domain..port.out..");

    @ArchTest
    static final ArchRule persistence_reaches_the_domain_through_outbound_ports_only = noClasses()
            .that()
            .resideInAPackage("..civicgrant.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..civicgrant.domain..service..", "..civicgrant.domain..port.in..");

    @ArchTest
    static final ArchRule persistence_classes_are_package_private = classes()
            .that()
            .resideInAPackage("..civicgrant.persistence..")
            .and()
            .areNotAnnotatedWith(StaticMetamodel.class)
            .and()
            .doNotBelongToAnyOf(DocumentBlobNames.class)
            .should()
            .notBePublic();

    @ArchTest
    static final ArchRule only_persistence_uses_jpa = noClasses()
            .that()
            .resideOutsideOfPackage("..civicgrant.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "org.hibernate..");

    @ArchTest
    static final ArchRule only_the_application_class_lives_in_the_root_package = classes()
            .that()
            .resideInAPackage("be.achzak.civicgrant")
            .should()
            .haveSimpleName(CivicgrantApplication.class.getSimpleName());

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

    private static ArchCondition<JavaClass> declareExactlyOneMethod() {
        return new ArchCondition<>("declare exactly one method") {
            @Override
            public void check(JavaClass useCase, ConditionEvents events) {
                int methods = useCase.getMethods().size();
                events.add(new SimpleConditionEvent(
                        useCase, methods == 1, useCase.getName() + " declares " + methods + " methods"));
            }
        };
    }

    private static ArchCondition<JavaClass> implementExactlyOneUseCase() {
        return new ArchCondition<>("implement exactly one use case and nothing else") {
            @Override
            public void check(JavaClass service, ConditionEvents events) {
                List<JavaClass> interfaces = List.copyOf(service.getRawInterfaces());
                boolean implementsOne = interfaces.size() == 1
                        && JavaClass.Predicates.resideInAPackage("..port.in..").test(interfaces.getFirst());
                events.add(new SimpleConditionEvent(
                        service, implementsOne, service.getName() + " implements " + interfaces));
            }
        };
    }

    // Every aggregate records domain events, so pullEvents is what tells one apart from a value object.
    private static DescribedPredicate<JavaMethodCall> aggregateBehaviour() {
        return DescribedPredicate.describe(
                "aggregate behaviour (anything but an accessor or a static factory)",
                call -> recordsEvents(call.getTargetOwner()) && !isAccessorOrFactory(call.getTarget()));
    }

    private static DescribedPredicate<JavaMethodCall> aggregateRestore() {
        return DescribedPredicate.describe(
                "an aggregate's restore",
                call -> recordsEvents(call.getTargetOwner())
                        && call.getTarget().getName().equals("restore"));
    }

    private static boolean recordsEvents(JavaClass type) {
        return type.getMethods().stream().anyMatch(method -> method.getName().equals("pullEvents"));
    }

    private static boolean isAccessorOrFactory(MethodCallTarget method) {
        boolean isStatic = method.resolveMember()
                .map(member -> member.getModifiers().contains(JavaModifier.STATIC))
                .orElse(false);
        boolean isAccessor =
                method.getRawParameterTypes().isEmpty() && !method.getName().equals("pullEvents");
        return isStatic || isAccessor;
    }

    private static ArchCondition<JavaMethod> takeExactlyOneCommandOrQuery() {
        return new ArchCondition<>("take exactly one command or query") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaClass> parameters = method.getRawParameterTypes();
                boolean takesOne = parameters.size() == 1
                        && (parameters.getFirst().isAssignableTo(Command.class)
                                || parameters.getFirst().isAssignableTo(Query.class));
                events.add(new SimpleConditionEvent(
                        method,
                        takesOne,
                        method.getFullName() + (takesOne ? " takes" : " doesn't take")
                                + " exactly one command or query"));
            }
        };
    }

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
