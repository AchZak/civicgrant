package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.CLOSES_AT;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.OPENS_AT;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.core.grantapplication.domain.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.core.organisation.domain.OrganisationBuilder.anOrganisation;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationCreated;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Creating a grant application")
class GrantApplicationCreationTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final ApplicationReference REFERENCE = new ApplicationReference("CG-2026-000042");

    private final FundingCall call = aFundingCall().build();

    private GrantApplication create(Organisation organisation, ProjectProposal proposal, Instant now) {
        return GrantApplication.create(
                REFERENCE, call, organisation, proposal, EligibilityPolicy.nonProfits(), anApplicant(), now);
    }

    private static ProjectProposal requesting(String requestedAmount) {
        return aProjectProposal()
                .withRequestedAmount(euros(requestedAmount))
                .withTotalProjectBudget(euros("20000.00"))
                .build();
    }

    @Test
    @DisplayName("starts as a draft of its applicant in the given call")
    void startsAsDraftOfItsApplicant() {
        // when
        GrantApplication application =
                create(anOrganisation().build(), aProjectProposal().build(), NOW);

        // then
        assertThat(application.status()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(application.applicant()).isEqualTo(anApplicant().id());
        assertThat(application.fundingCallId()).isEqualTo(call.id());
        assertThat(application.submittedAt()).isEmpty();
        assertThat(application.documents()).isEmpty();
    }

    @Test
    @DisplayName("records ApplicationCreated")
    void recordsApplicationCreated() {
        // given
        Organisation organisation = anOrganisation().build();

        // when
        GrantApplication application = create(organisation, aProjectProposal().build(), NOW);

        // then
        assertThat(application.pullEvents())
                .containsExactly(new ApplicationCreated(
                        application.id(),
                        call.id(),
                        organisation.id(),
                        anApplicant().id(),
                        NOW));
    }

    @Test
    @DisplayName("may be prepared before the call opens")
    void mayBePreparedBeforeTheCallOpens() {
        // given
        Instant aWeekBeforeOpening = OPENS_AT.minus(Duration.ofDays(7));

        // when
        GrantApplication application =
                create(anOrganisation().build(), aProjectProposal().build(), aWeekBeforeOpening);

        // then
        assertThat(application.status()).isEqualTo(ApplicationStatus.DRAFT);
    }

    @Test
    @DisplayName("rejects a new draft once the call has closed")
    void rejectsNewDraftOnceTheCallHasClosed() {
        // when
        Throwable thrown = catchThrowable(
                () -> create(anOrganisation().build(), aProjectProposal().build(), CLOSES_AT));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_CLOSED");
    }

    @Test
    @DisplayName("rejects an organisation whose legal form isn't eligible")
    void rejectsOrganisationWhoseLegalFormIsNotEligible() {
        // given
        Organisation company = anOrganisation().withLegalForm(LegalForm.COMPANY).build();

        // when
        Throwable thrown =
                catchThrowable(() -> create(company, aProjectProposal().build(), NOW));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("LEGAL_FORM_NOT_ELIGIBLE");
    }

    @Test
    @DisplayName("accepts a requested amount of exactly the call's cap")
    void acceptsRequestedAmountOfExactlyTheCap() {
        // given
        ProjectProposal atTheCap = requesting("15000.00");

        // when
        GrantApplication application = create(anOrganisation().build(), atTheCap, NOW);

        // then
        assertThat(application.proposal()).isEqualTo(atTheCap);
    }

    @Test
    @DisplayName("rejects a requested amount one cent above the call's cap")
    void rejectsRequestedAmountOneCentAboveTheCap() {
        // given
        ProjectProposal aboveTheCap = requesting("15000.01");

        // when
        Throwable thrown = catchThrowable(() -> create(anOrganisation().build(), aboveTheCap, NOW));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("REQUESTED_AMOUNT_EXCEEDS_CAP");
    }
}
