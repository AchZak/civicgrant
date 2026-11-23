package be.achzak.civicgrant.core.grantapplication.application.service;

import static be.achzak.civicgrant.core.common.domain.error.DomainExceptionAssert.assertThatDomainException;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.actor;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.anAssessor;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.anAssessedApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.Assessment;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Getting an assessment")
class GetAssessmentServiceTest {

    private static final GrantApplication ASSESSED = anAssessedApplication().build();

    private final GetAssessmentService service =
            new GetAssessmentService(new InMemoryGrantApplicationRepository(ASSESSED));

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"ASSESSOR", "DECISION_MAKER"})
    @DisplayName("gives staff the assessment of an assessed application")
    void givesStaffTheAssessmentOfAnAssessedApplication(Role role) {
        // when
        Assessment found = service.get(new GetAssessmentQuery(ASSESSED.id(), actor("staff-1", role)));

        // then
        assertThat(ASSESSED.assessment()).contains(found);
    }

    @Test
    @DisplayName("reports an unknown application as not found")
    void reportsAnUnknownApplicationAsNotFound() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.get(new GetAssessmentQuery(ApplicationId.generate(), anAssessor())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("refuses an actor with only the applicant role, even for their own application")
    void refusesAnActorWithOnlyTheApplicantRole() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.get(new GetAssessmentQuery(ASSESSED.id(), actor("applicant-1", Role.APPLICANT))));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
