package be.achzak.civicgrant.core.grantapplication.application.service;

import static be.achzak.civicgrant.core.common.domain.error.DomainExceptionAssert.assertThatDomainException;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.anApplicant;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.PdfFiles.aPng;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.common.application.port.out.RecordingDomainEventPublisher;
import be.achzak.civicgrant.core.fundingcall.application.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.application.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryDocumentStorage;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Attaching a supporting document")
class AttachDocumentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("stores no file and publishes nothing when the domain refuses the document")
    void storesNoFileAndPublishesNothingWhenTheDomainRefusesTheDocument() {
        // given
        FundingCall fundingCall = aFundingCall().build();
        GrantApplication draft =
                aDraftApplication().forCall(fundingCall).withoutDocuments().build();
        InMemoryDocumentStorage documents = new InMemoryDocumentStorage();
        RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
        AttachDocumentService service = new AttachDocumentService(
                new InMemoryGrantApplicationRepository(draft),
                new InMemoryFundingCallRepository(fundingCall),
                documents,
                events,
                CLOCK);
        AttachDocumentCommand png =
                new AttachDocumentCommand(draft.id(), "begroting.pdf", "application/pdf", aPng(), anApplicant());

        // when
        Throwable thrown = catchThrowable(() -> service.attach(png));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NOT_PDF");
        assertThat(documents.isEmpty()).isTrue();
        assertThat(events.published()).isEmpty();
    }
}
