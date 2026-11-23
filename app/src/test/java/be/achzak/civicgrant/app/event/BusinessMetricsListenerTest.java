package be.achzak.civicgrant.app.event;

import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationApproved;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationCreated;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationEdited;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationRejected;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationSubmitted;
import be.achzak.civicgrant.core.grantapplication.domain.event.AssessmentRecorded;
import be.achzak.civicgrant.core.grantapplication.domain.event.DocumentAttached;
import be.achzak.civicgrant.core.grantapplication.domain.event.DocumentRemoved;
import be.achzak.civicgrant.core.grantapplication.domain.event.GrantApplicationEvent;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Business metrics")
class BusinessMetricsListenerTest {

    private static final ApplicationId APPLICATION = ApplicationId.generate();
    private static final UserId ACTOR = new UserId("user-1");
    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BusinessMetricsListener listener = new BusinessMetricsListener(registry);

    @Nested
    @DisplayName("Counting applications")
    class CountingApplications {

        @Test
        @DisplayName("shows every workflow step at zero before anything happens")
        void showsEveryWorkflowStepAtZeroBeforeAnythingHappens() {
            // when
            Map<String, Double> counts = applicationCounts();

            // then
            assertThat(counts).isEqualTo(noApplicationsCounted());
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("be.achzak.civicgrant.app.event.BusinessMetricsListenerTest#workflowSteps")
        @DisplayName("counts a workflow step under its own tag only")
        void countsAWorkflowStepUnderItsOwnTagOnly(GrantApplicationEvent event, String step) {
            // given
            Map<String, Double> expected = noApplicationsCounted();
            expected.put(step, 1.0);

            // when
            listener.on(event);

            // then
            assertThat(applicationCounts()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("be.achzak.civicgrant.app.event.BusinessMetricsListenerTest#changesWithinAStep")
        @DisplayName("leaves changes within a step uncounted")
        void leavesChangesWithinAStepUncounted(GrantApplicationEvent event) {
            // when
            listener.on(event);

            // then
            assertThat(applicationCounts()).isEqualTo(noApplicationsCounted());
        }
    }

    @Nested
    @DisplayName("Measuring granted amounts")
    class MeasuringGrantedAmounts {

        @Test
        @DisplayName("records the amount of each approval in euros")
        void recordsTheAmountOfEachApprovalInEuros() {
            // when
            listener.on(new ApplicationApproved(APPLICATION, euros("7000.00"), ACTOR, NOW));
            listener.on(new ApplicationApproved(APPLICATION, euros("2500.50"), ACTOR, NOW));

            // then
            DistributionSummary amounts = grantedAmounts();
            assertThat(amounts.count()).isEqualTo(2);
            assertThat(amounts.totalAmount()).isEqualTo(9500.50);
            assertThat(amounts.max()).isEqualTo(7000.00);
            assertThat(amounts.getId().getBaseUnit()).isEqualTo("euros");
        }

        @Test
        @DisplayName("records no amount for a rejection")
        void recordsNoAmountForARejection() {
            // when
            listener.on(new ApplicationRejected(APPLICATION, RejectionGround.INELIGIBLE, ACTOR, NOW));

            // then
            assertThat(grantedAmounts().count()).isZero();
        }
    }

    static Stream<Arguments> workflowSteps() {
        return Stream.of(
                Arguments.of(
                        new ApplicationCreated(
                                APPLICATION, FundingCallId.generate(), OrganisationId.generate(), ACTOR, NOW),
                        "created"),
                Arguments.of(new ApplicationSubmitted(APPLICATION, euros("8000.00"), ACTOR, NOW), "submitted"),
                Arguments.of(
                        new AssessmentRecorded(
                                APPLICATION, true, new Recommendation.Approve(euros("7500.00")), ACTOR, NOW),
                        "assessed"),
                Arguments.of(new ApplicationApproved(APPLICATION, euros("7000.00"), ACTOR, NOW), "approved"),
                Arguments.of(
                        new ApplicationRejected(APPLICATION, RejectionGround.INSUFFICIENT_QUALITY, ACTOR, NOW),
                        "rejected"));
    }

    static Stream<Arguments> changesWithinAStep() {
        return Stream.of(
                Arguments.of(named("edited", new ApplicationEdited(APPLICATION, euros("8000.00"), ACTOR, NOW))),
                Arguments.of(named(
                        "document attached", new DocumentAttached(APPLICATION, DocumentId.generate(), ACTOR, NOW))),
                Arguments.of(named(
                        "document removed", new DocumentRemoved(APPLICATION, DocumentId.generate(), ACTOR, NOW))));
    }

    private Map<String, Double> applicationCounts() {
        return registry.get(BusinessMetricsListener.APPLICATIONS).counters().stream()
                .collect(Collectors.toMap(counter -> counter.getId().getTag("event"), Counter::count));
    }

    private DistributionSummary grantedAmounts() {
        return registry.get(BusinessMetricsListener.GRANTED_AMOUNTS).summary();
    }

    private static Map<String, Double> noApplicationsCounted() {
        return new HashMap<>(
                Map.of("created", 0.0, "submitted", 0.0, "assessed", 0.0, "approved", 0.0, "rejected", 0.0));
    }
}
