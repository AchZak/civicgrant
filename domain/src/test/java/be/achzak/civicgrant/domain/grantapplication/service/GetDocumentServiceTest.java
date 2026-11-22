package be.achzak.civicgrant.domain.grantapplication.service;

import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSupportingDocument;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anotherApplicant;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetDocumentQuery;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Getting a supporting document")
class GetDocumentServiceTest {

    private static final SupportingDocument DOCUMENT = aSupportingDocument();
    private static final GrantApplication DRAFT =
            aDraftApplication().withDocuments(DOCUMENT).build();
    private static final GrantApplication SUBMITTED =
            aSubmittedApplication().withDocuments(DOCUMENT).build();

    private final GetDocumentService service =
            new GetDocumentService(new InMemoryGrantApplicationRepository(DRAFT, SUBMITTED));

    @Test
    @DisplayName("gives the applicant a document of their own draft")
    void givesTheApplicantADocumentOfTheirOwnDraft() {
        // when
        SupportingDocument found = service.get(new GetDocumentQuery(DRAFT.id(), DOCUMENT.id(), anApplicant()));

        // then
        assertThat(found).isEqualTo(DOCUMENT);
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"ASSESSOR", "DECISION_MAKER"})
    @DisplayName("gives staff a document of a submitted application")
    void givesStaffADocumentOfASubmittedApplication(Role role) {
        // when
        SupportingDocument found =
                service.get(new GetDocumentQuery(SUBMITTED.id(), DOCUMENT.id(), actor("staff-1", role)));

        // then
        assertThat(found).isEqualTo(DOCUMENT);
    }

    @Test
    @DisplayName("hides a document from another applicant, as if the application didn't exist")
    void hidesADocumentFromAnotherApplicant() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.get(new GetDocumentQuery(DRAFT.id(), DOCUMENT.id(), anotherApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("reports an unknown application as not found")
    void reportsAnUnknownApplicationAsNotFound() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.get(new GetDocumentQuery(ApplicationId.generate(), DOCUMENT.id(), anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("refuses an actor without any role")
    void refusesAnActorWithoutAnyRole() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.get(new GetDocumentQuery(DRAFT.id(), DOCUMENT.id(), anActorWithoutRoles())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
