package be.achzak.civicgrant.domain.organisation.service;

import static be.achzak.civicgrant.domain.organisation.model.OrganisationBuilder.anOrganisation;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.domain.organisation.port.out.InMemoryOrganisationRepository;
import be.achzak.civicgrant.domain.shared.Actor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Registering an organisation")
class RegisterOrganisationServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final EnterpriseNumber ENTERPRISE_NUMBER = new EnterpriseNumber("0202239951");

    private static RegisterOrganisationCommand registrationBy(Actor actor) {
        return new RegisterOrganisationCommand(ENTERPRISE_NUMBER, "Buurtwerk vzw", LegalForm.VZW_ASBL, actor);
    }

    @Test
    @DisplayName("saves the new organisation and returns its id")
    void savesTheNewOrganisationAndReturnsItsId() {
        // given
        InMemoryOrganisationRepository organisations = new InMemoryOrganisationRepository();
        RegisterOrganisationService service = new RegisterOrganisationService(organisations, CLOCK);

        // when
        OrganisationId id = service.register(registrationBy(anApplicant()));

        // then
        assertThat(organisations.findById(id))
                .get()
                .extracting(
                        Organisation::enterpriseNumber,
                        Organisation::name,
                        Organisation::legalForm,
                        Organisation::createdAt)
                .containsExactly(ENTERPRISE_NUMBER, "Buurtwerk vzw", LegalForm.VZW_ASBL, NOW);
    }

    @Test
    @DisplayName("refuses an enterprise number that is already registered")
    void refusesEnterpriseNumberThatIsAlreadyRegistered() {
        // given
        InMemoryOrganisationRepository organisations = new InMemoryOrganisationRepository(
                anOrganisation().withEnterpriseNumber(ENTERPRISE_NUMBER).build());
        RegisterOrganisationService service = new RegisterOrganisationService(organisations, CLOCK);

        // when
        Throwable thrown = catchThrowable(() -> service.register(registrationBy(anApplicant())));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("ORGANISATION_ALREADY_REGISTERED");
    }

    @Test
    @DisplayName("refuses an actor without the applicant role, saving nothing")
    void refusesActorWithoutTheApplicantRole() {
        // given
        InMemoryOrganisationRepository organisations = new InMemoryOrganisationRepository();
        RegisterOrganisationService service = new RegisterOrganisationService(organisations, CLOCK);

        // when
        Throwable thrown = catchThrowable(() -> service.register(registrationBy(anAssessor())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
        assertThat(organisations.findByEnterpriseNumber(ENTERPRISE_NUMBER)).isEmpty();
    }
}
