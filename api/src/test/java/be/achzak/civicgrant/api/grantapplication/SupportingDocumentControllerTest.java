package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.APPLICATION_ID;
import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.APPLICATION_NOT_DRAFT;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.APPLICATION_NOT_FOUND;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.DOCUMENT_NOT_PDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import be.achzak.civicgrant.api.security.ApiWebMvcTest;
import be.achzak.civicgrant.core.common.domain.error.BusinessRuleViolationException;
import be.achzak.civicgrant.core.common.domain.error.ConflictException;
import be.achzak.civicgrant.core.common.domain.error.NotFoundException;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.grantapplication.application.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.AttachDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.DocumentDownload;
import be.achzak.civicgrant.core.grantapplication.application.port.in.DownloadDocumentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.DownloadDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetDocumentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RemoveDocumentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RemoveDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiWebMvcTest(SupportingDocumentController.class)
@DisplayName("Supporting document endpoints")
class SupportingDocumentControllerTest {

    private static final byte[] PDF = "%PDF-1.7\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private static final DocumentId DOCUMENT_ID =
            new DocumentId(UUID.fromString("a9917c01-0004-4000-8000-000000000004"));
    private static final Instant UPLOADED_AT = Instant.parse("2026-04-14T11:15:00Z");
    private static final SupportingDocument DOCUMENT = new SupportingDocument(
            DOCUMENT_ID, "begroting.pdf", PDF.length, new UserId("3f1c2b7e-5a4d-4c3b-9e2f-1a0b9c8d7e6f"), UPLOADED_AT);
    private static final String DOCUMENTS = "/api/v1/applications/" + APPLICATION_ID.value() + "/documents";
    private static final String DOCUMENT_PATH = DOCUMENTS + "/" + DOCUMENT_ID.value();

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AttachDocumentUseCase attachDocument;

    @MockitoBean
    private GetDocumentUseCase getDocument;

    @MockitoBean
    private DownloadDocumentUseCase downloadDocument;

    @MockitoBean
    private RemoveDocumentUseCase removeDocument;

    @Nested
    @DisplayName("Attaching a document")
    class Attaching {

        @Test
        @DisplayName("attaches the file and answers 201 with its absolute location and the stored details")
        void attachesTheFileAndAnswers201WithItsLocationAndTheStoredDetails() {
            // given
            given(attachDocument.attach(new AttachDocumentCommand(
                            APPLICATION_ID, "begroting.pdf", "application/pdf", PDF, actor(Role.APPLICANT))))
                    .willReturn(DOCUMENT_ID);
            given(getDocument.get(new GetDocumentQuery(APPLICATION_ID, DOCUMENT_ID, actor(Role.APPLICANT))))
                    .willReturn(DOCUMENT);

            // when
            MvcTestResult result = upload(new MockMultipartFile("file", "begroting.pdf", "application/pdf", PDF));

            // then
            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).headers().hasValue("Location", "http://localhost" + DOCUMENT_PATH);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.id")
                    .asString()
                    .isEqualTo(DOCUMENT_ID.value().toString());
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.fileName")
                    .asString()
                    .isEqualTo("begroting.pdf");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.sizeInBytes")
                    .asNumber()
                    .isEqualTo(PDF.length);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.uploadedAt")
                    .asString()
                    .satisfies(text ->
                            assertThat(OffsetDateTime.parse(text).toInstant()).isEqualTo(UPLOADED_AT));
        }

        @Test
        @DisplayName("hands a file part without content type to the domain, which refuses it as not a PDF")
        void handsAFilePartWithoutContentTypeToTheDomain() {
            // given
            given(attachDocument.attach(
                            new AttachDocumentCommand(APPLICATION_ID, "begroting.pdf", "", PDF, actor(Role.APPLICANT))))
                    .willThrow(new BusinessRuleViolationException(DOCUMENT_NOT_PDF, "No content type declared"));

            // when
            MvcTestResult result = upload(new MockMultipartFile("file", "begroting.pdf", null, PDF));

            // then
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:documentNotPdf");
        }

        @Test
        @DisplayName("answers 400 badRequest on the file part when it's missing, without calling the use case")
        void answers400WhenTheFilePartIsMissing() {
            // when
            MvcTestResult result = upload(new MockMultipartFile("attachment", "begroting.pdf", "application/pdf", PDF));

            // then
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:badRequest");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.issues[0].name")
                    .asString()
                    .isEqualTo("/file");
            then(attachDocument).shouldHaveNoInteractions();
        }

        private MvcTestResult upload(MockMultipartFile file) {
            return mvc.post()
                    .uri(DOCUMENTS)
                    .multipart()
                    .file(file)
                    .with(accessToken(Role.APPLICANT))
                    .exchange();
        }
    }

    @Nested
    @DisplayName("Getting and downloading a document")
    class Reading {

        @Test
        @DisplayName("returns the document's details")
        void returnsTheDocumentsDetails() {
            // given
            given(getDocument.get(new GetDocumentQuery(APPLICATION_ID, DOCUMENT_ID, actor(Role.ASSESSOR))))
                    .willReturn(DOCUMENT);

            // when
            MvcTestResult result = mvc.get()
                    .uri(DOCUMENT_PATH)
                    .with(accessToken(Role.ASSESSOR))
                    .exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.fileName")
                    .asString()
                    .isEqualTo("begroting.pdf");
        }

        @Test
        @DisplayName("returns 404 when the document is hidden or missing")
        void returns404WhenTheDocumentIsHiddenOrMissing() {
            // given
            given(getDocument.get(new GetDocumentQuery(APPLICATION_ID, DOCUMENT_ID, actor(Role.APPLICANT))))
                    .willThrow(new NotFoundException(APPLICATION_NOT_FOUND, "Not found"));

            // when
            MvcTestResult result = mvc.get()
                    .uri(DOCUMENT_PATH)
                    .with(accessToken(Role.APPLICANT))
                    .exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:resourceNotFound");
        }

        @Test
        @DisplayName("streams the PDF with its type, length and file name as an attachment")
        void streamsThePdfWithItsTypeLengthAndFileName() {
            // given
            given(downloadDocument.download(
                            new DownloadDocumentQuery(APPLICATION_ID, DOCUMENT_ID, actor(Role.ASSESSOR))))
                    .willReturn(new DocumentDownload(DOCUMENT, new ByteArrayInputStream(PDF)));

            // when
            MvcTestResult result = mvc.get()
                    .uri(DOCUMENT_PATH + "/content")
                    .with(accessToken(Role.ASSESSOR))
                    .exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_PDF);
            assertThat(result).headers().hasValue("Content-Length", String.valueOf(PDF.length));
            assertThat(result)
                    .headers()
                    .hasValue(
                            "Content-Disposition",
                            "attachment; filename=\"begroting.pdf\"; filename*=UTF-8''begroting.pdf");
            assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(PDF);
        }
    }

    @Nested
    @DisplayName("Removing a document")
    class Removing {

        @Test
        @DisplayName("removes the document and answers 204 without a body")
        void removesTheDocumentAndAnswers204() {
            // when
            MvcTestResult result = mvc.delete()
                    .uri(DOCUMENT_PATH)
                    .with(accessToken(Role.APPLICANT))
                    .exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
            assertThat(result.getResponse().getContentAsByteArray()).isEmpty();
            then(removeDocument)
                    .should()
                    .remove(new RemoveDocumentCommand(APPLICATION_ID, DOCUMENT_ID, actor(Role.APPLICANT)));
        }

        @Test
        @DisplayName("answers 409 applicationNotDraft once the application is submitted")
        void answers409OnceTheApplicationIsSubmitted() {
            // given
            willThrow(new ConflictException(APPLICATION_NOT_DRAFT, "Application is SUBMITTED, not DRAFT"))
                    .given(removeDocument)
                    .remove(new RemoveDocumentCommand(APPLICATION_ID, DOCUMENT_ID, actor(Role.APPLICANT)));

            // when
            MvcTestResult result = mvc.delete()
                    .uri(DOCUMENT_PATH)
                    .with(accessToken(Role.APPLICANT))
                    .exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:applicationNotDraft");
        }
    }
}
