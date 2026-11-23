package be.achzak.civicgrant;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import be.achzak.civicgrant.core.audit.application.service.RecordAuditEntryService;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@AnalyzeClasses(packages = "be.achzak.civicgrant", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..civicgrant.core.*.domain..";
    private static final String EVENTS = "..civicgrant.core.*.domain.event..";
    private static final String APPLICATION = "..civicgrant.core.*.application..";
    private static final String INBOUND_PORTS = "..civicgrant.core.*.application.port.in..";
    private static final String OUTBOUND_PORTS = "..civicgrant.core.*.application.port.out..";
    private static final String SERVICES = "..civicgrant.core.*.application.service..";
    private static final String PERSISTENCE = "..civicgrant.persistence..";
    private static final String DOCUMENT_STORAGE = "..civicgrant.documentstorage..";
    private static final Set<String> ADAPTERS = Set.of("api", "persistence", "documentstorage");

    @ArchTest
    static final ArchRule the_domain_depends_only_on_the_jdk_and_jspecify = classes()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "org.jspecify..", "..civicgrant.core..");

    // ArchUnit also counts the annotations' member types and Lombok's marker on generated constructors.
    @ArchTest
    static final ArchRule the_application_uses_spring_only_for_service_and_transactional = classes()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .onlyDependOnClassesThat(JavaClass.Predicates.resideInAnyPackage(
                            "java..", "org.jspecify..", "..civicgrant.core..")
                    .or(JavaClass.Predicates.belongToAnyOf(
                            Service.class, Transactional.class, Propagation.class, Isolation.class, Generated.class)));

    @ArchTest
    static final ArchRule the_application_and_events_leave_null_checks_to_nullaway = noClasses()
            .that()
            .resideInAnyPackage(APPLICATION, EVENTS)
            .should()
            .callMethodWhere(DescribedPredicate.describe(
                    "Objects.requireNonNull",
                    call -> call.getTargetOwner().isEquivalentTo(Objects.class)
                            && call.getTarget().getName().equals("requireNonNull")));

    @ArchTest
    static final ArchRule every_core_class_belongs_to_the_domain_or_the_application =
            classes().that().resideInAPackage("..civicgrant.core..").should().resideInAnyPackage(DOMAIN, APPLICATION);

    @ArchTest
    static final ArchRule the_domain_does_not_depend_on_the_application = noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(APPLICATION);

    @ArchTest
    static final ArchRule shared_kernel_depends_on_no_aggregate = classes()
            .that()
            .resideInAPackage("..civicgrant.core.sharedkernel..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "..civicgrant.core.sharedkernel..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule no_cycles_between_aggregate_packages =
            slices().matching("..civicgrant.core.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule nothing_else_in_the_core_depends_on_audit = noClasses()
            .that()
            .resideInAPackage("..civicgrant.core..")
            .and()
            .resideOutsideOfPackage("..civicgrant.core.audit..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..civicgrant.core.audit..");

    @ArchTest
    static final ArchRule every_use_case_checks_the_actors_role = methods()
            .that()
            .arePublic()
            .and()
            .areDeclaredInClassesThat()
            .resideInAPackage(SERVICES)
            .and()
            .areNotDeclaredIn(RecordAuditEntryService.class)
            .should(callActorRequireAnyRole());

    @ArchTest
    static final ArchRule every_use_case_takes_exactly_one_command_or_query = methods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage(INBOUND_PORTS)
            .and()
            .areDeclaredInClassesThat()
            .areInterfaces()
            .should(takeExactlyOneCommandOrQuery());

    @ArchTest
    static final ArchRule use_cases_are_interfaces_with_one_method = classes()
            .that()
            .resideInAPackage(INBOUND_PORTS)
            .and()
            .areInterfaces()
            .should()
            .haveSimpleNameEndingWith("UseCase")
            .andShould(declareExactlyOneMethod());

    @ArchTest
    static final ArchRule outbound_ports_are_interfaces =
            classes().that().resideInAPackage(OUTBOUND_PORTS).should().beInterfaces();

    @ArchTest
    static final ArchRule services_implement_exactly_one_use_case = classes()
            .that()
            .resideInAPackage(SERVICES)
            .should()
            .haveSimpleNameEndingWith("Service")
            .andShould(implementExactlyOneUseCase());

    @ArchTest
    static final ArchRule services_are_spring_services =
            classes().that().resideInAPackage(SERVICES).should().beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule commands_run_read_write_and_queries_read_only = methods()
            .that()
            .arePublic()
            .and()
            .areDeclaredInClassesThat()
            .resideInAPackage(SERVICES)
            .should(runInATransactionThatMatchesTheirRequest());

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
            .resideInAPackage(INBOUND_PORTS);

    @ArchTest
    static final ArchRule outside_the_core_aggregates_are_only_read_through_accessors = noClasses()
            .that()
            .resideOutsideOfPackage("..civicgrant.core..")
            .should()
            .callMethodWhere(aggregateBehaviour());

    @ArchTest
    static final ArchRule only_persistence_restores_aggregates =
            noClasses().that().resideOutsideOfPackage(PERSISTENCE).should().callMethodWhere(aggregateRestore());

    @ArchTest
    static final ArchRule api_reaches_the_core_through_use_cases_only = noClasses()
            .that()
            .resideInAPackage("..civicgrant.api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(SERVICES, OUTBOUND_PORTS);

    @ArchTest
    static final ArchRule outbound_adapters_reach_the_core_through_outbound_ports_only = noClasses()
            .that()
            .resideInAnyPackage(PERSISTENCE, DOCUMENT_STORAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(SERVICES, INBOUND_PORTS);

    @ArchTest
    static final ArchRule outbound_adapter_classes_are_package_private = classes()
            .that()
            .resideInAnyPackage(PERSISTENCE, DOCUMENT_STORAGE)
            .and()
            .areNotAnnotatedWith(StaticMetamodel.class)
            .should()
            .notBePublic();

    @ArchTest
    static final ArchRule adapters_do_not_depend_on_each_other = slices().matching("be.achzak.civicgrant.(*)..")
            .that(DescribedPredicate.describe("are adapters", slice -> ADAPTERS.contains(slice.getNamePart(1))))
            .should()
            .notDependOnEachOther();

    @ArchTest
    static final ArchRule only_persistence_uses_jpa = noClasses()
            .that()
            .resideOutsideOfPackage(PERSISTENCE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "org.hibernate..");

    @ArchTest
    static final ArchRule only_document_storage_uses_blob_storage = noClasses()
            .that()
            .resideOutsideOfPackage(DOCUMENT_STORAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.azure.storage..");

    @ArchTest
    static final ArchRule only_the_application_class_lives_in_the_root_package = classes()
            .that()
            .resideInAPackage("be.achzak.civicgrant")
            .should()
            .haveSimpleName(CivicgrantApplication.class.getSimpleName());

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
                        && JavaClass.Predicates.resideInAPackage(INBOUND_PORTS).test(interfaces.getFirst());
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

    private static ArchCondition<JavaMethod> runInATransactionThatMatchesTheirRequest() {
        return new ArchCondition<>("run in a read-write transaction for a command and a read-only one for a query") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean takesQuery =
                        method.getRawParameterTypes().stream().anyMatch(type -> type.isAssignableTo(Query.class));
                Optional<Transactional> transactional = method.tryGetAnnotationOfType(Transactional.class);
                boolean matches = transactional
                        .map(annotation -> annotation.readOnly() == takesQuery)
                        .orElse(false);
                events.add(new SimpleConditionEvent(
                        method,
                        matches,
                        method.getFullName() + (matches ? " runs" : " doesn't run") + " in a "
                                + (takesQuery ? "read-only" : "read-write") + " transaction"));
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
