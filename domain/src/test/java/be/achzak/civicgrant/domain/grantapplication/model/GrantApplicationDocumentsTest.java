package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.CLOSES_AT;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSupportingDocument;
import static be.achzak.civicgrant.domain.grantapplication.model.PdfFiles.aPdf;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.Actors.anotherApplicant;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.grantapplication.model.event.DocumentAttached;
import be.achzak.civicgrant.domain.grantapplication.model.event.DocumentRemoved;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Supporting documents of a grant application")
class GrantApplicationDocumentsTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

    private final FundingCall call = aFundingCall().build();

    @Nested
    @DisplayName("Attaching and removing a document")
    class SupportingDocuments {

        private static final String PDF = "application/pdf";

        @Test
        @DisplayName("attaches a document to a draft")
        void attachesDocumentToDraft() {
            // given
            GrantApplication application =
                    aDraftApplication().forCall(call).withoutDocuments().build();

            // when
            SupportingDocument document =
                    application.attachDocument("begroting.pdf", PDF, aPdf(), call, anApplicant(), NOW);

            // then
            assertThat(application.documents()).containsExactly(document);
            assertThat(document.fileName()).isEqualTo("begroting.pdf");
            assertThat(document.uploadedBy()).isEqualTo(anApplicant().id());
            assertThat(document.uploadedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("records DocumentAttached")
        void recordsDocumentAttached() {
            // given
            GrantApplication application =
                    aDraftApplication().forCall(call).withoutDocuments().build();

            // when
            SupportingDocument document =
                    application.attachDocument("begroting.pdf", PDF, aPdf(), call, anApplicant(), NOW);

            // then
            assertThat(application.pullEvents())
                    .containsExactly(new DocumentAttached(
                            application.id(), document.id(), anApplicant().id(), NOW));
        }

        @Test
        @DisplayName("leaves the draft unchanged when the document is refused")
        void leavesTheDraftUnchangedWhenTheDocumentIsRefused() {
            // given
            GrantApplication application =
                    aDraftApplication().forCall(call).withoutDocuments().build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.attachDocument("foto.png", "image/png", aPdf(), call, anApplicant(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NOT_PDF");
            assertThat(application.documents()).isEmpty();
            assertThat(application.pullEvents()).isEmpty();
        }

        @Test
        @DisplayName("rejects a document for a submitted application")
        void rejectsDocumentForSubmittedApplication() {
            // given
            GrantApplication application = aSubmittedApplication().forCall(call).build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.attachDocument("begroting.pdf", PDF, aPdf(), call, anApplicant(), NOW));

            // then
            assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_DRAFT");
        }

        @Test
        @DisplayName("rejects a new document once the call has closed")
        void rejectsNewDocumentOnceTheCallHasClosed() {
            // given
            GrantApplication application = aDraftApplication().forCall(call).build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.attachDocument("begroting.pdf", PDF, aPdf(), call, anApplicant(), CLOSES_AT));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_CLOSED");
        }

        @Test
        @DisplayName("removes a document from a draft")
        void removesDocumentFromDraft() {
            // given
            SupportingDocument document = aSupportingDocument();
            GrantApplication application =
                    aDraftApplication().withDocuments(document).build();

            // when
            application.removeDocument(document.id(), anApplicant(), NOW);

            // then
            assertThat(application.documents()).isEmpty();
            assertThat(application.updatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("records DocumentRemoved")
        void recordsDocumentRemoved() {
            // given
            SupportingDocument document = aSupportingDocument();
            GrantApplication application =
                    aDraftApplication().withDocuments(document).build();

            // when
            application.removeDocument(document.id(), anApplicant(), NOW);

            // then
            assertThat(application.pullEvents())
                    .containsExactly(new DocumentRemoved(
                            application.id(), document.id(), anApplicant().id(), NOW));
        }

        @Test
        @DisplayName("answers not found for a document the draft doesn't have")
        void answersNotFoundForUnknownDocument() {
            // given
            GrantApplication application = aDraftApplication().build();

            // when
            Throwable thrown =
                    catchThrowable(() -> application.removeDocument(DocumentId.generate(), anApplicant(), NOW));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("DOCUMENT_NOT_FOUND");
        }

        @Test
        @DisplayName("keeps the documents of a submitted application")
        void keepsTheDocumentsOfSubmittedApplication() {
            // given
            SupportingDocument document = aSupportingDocument();
            GrantApplication application =
                    aSubmittedApplication().withDocuments(document).build();

            // when
            Throwable thrown = catchThrowable(() -> application.removeDocument(document.id(), anApplicant(), NOW));

            // then
            assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_DRAFT");
            assertThat(application.documents()).containsExactly(document);
        }
    }

    @Nested
    @DisplayName("Reading a document")
    class ReadDocument {

        private final SupportingDocument document = aSupportingDocument();

        @Test
        @DisplayName("gives the applicant a document of their own draft")
        void givesTheApplicantADocumentOfTheirOwnDraft() {
            // given
            GrantApplication application =
                    aDraftApplication().withDocuments(document).build();

            // when
            SupportingDocument read = application.document(document.id(), anApplicant());

            // then
            assertThat(read).isEqualTo(document);
        }

        @Test
        @DisplayName("gives staff a document of a submitted application")
        void givesStaffADocumentOfASubmittedApplication() {
            // given
            GrantApplication application =
                    aSubmittedApplication().withDocuments(document).build();

            // when
            SupportingDocument read = application.document(document.id(), anAssessor());

            // then
            assertThat(read).isEqualTo(document);
        }

        @Test
        @DisplayName("hides the documents of a draft from staff, as if it didn't exist")
        void hidesTheDocumentsOfADraftFromStaff() {
            // given
            GrantApplication application =
                    aDraftApplication().withDocuments(document).build();

            // when
            Throwable thrown = catchThrowable(() -> application.document(document.id(), anAssessor()));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
        }

        @Test
        @DisplayName("hides the documents from another applicant, as if the application didn't exist")
        void hidesTheDocumentsFromAnotherApplicant() {
            // given
            GrantApplication application =
                    aSubmittedApplication().withDocuments(document).build();

            // when
            Throwable thrown = catchThrowable(() -> application.document(document.id(), anotherApplicant()));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
        }

        @Test
        @DisplayName("answers not found for a document the application doesn't have")
        void answersNotFoundForADocumentTheApplicationDoesNotHave() {
            // given
            GrantApplication application =
                    aDraftApplication().withDocuments(document).build();

            // when
            Throwable thrown = catchThrowable(() -> application.document(DocumentId.generate(), anApplicant()));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("DOCUMENT_NOT_FOUND");
        }
    }
}
