package be.achzak.civicgrant.persistence.organisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.out.OrganisationRepository;
import be.achzak.civicgrant.domain.shared.ConflictException;
import be.achzak.civicgrant.persistence.PersistenceTest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@PersistenceTest
@DisplayName("Organisation persistence")
class OrganisationPersistenceAdapterTest {

    private static final EnterpriseNumber ENTERPRISE_NUMBER = new EnterpriseNumber("0202239951");

    @Autowired
    private OrganisationRepository organisations;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("saves an organisation and loads it back with every column")
    void savesAnOrganisationAndLoadsItBackWithEveryColumn() {
        // given
        Organisation organisation = organisation(ENTERPRISE_NUMBER);
        organisations.save(organisation);
        entityManager.clear();

        // when
        Optional<Organisation> loaded = organisations.findById(organisation.id());

        // then
        assertThat(loaded).get().usingRecursiveComparison().isEqualTo(organisation);
    }

    @Test
    @DisplayName("finds an organisation by its enterprise number")
    void findsAnOrganisationByItsEnterpriseNumber() {
        // given
        Organisation organisation = organisation(ENTERPRISE_NUMBER);
        organisations.save(organisation(new EnterpriseNumber("0123456749")));
        organisations.save(organisation);
        entityManager.clear();

        // when
        Optional<Organisation> found = organisations.findByEnterpriseNumber(ENTERPRISE_NUMBER);

        // then
        assertThat(found).get().extracting(Organisation::id).isEqualTo(organisation.id());
    }

    @Test
    @DisplayName("finds nothing for an enterprise number nobody registered")
    void findsNothingForAnEnterpriseNumberNobodyRegistered() {
        // when
        Optional<Organisation> found = organisations.findByEnterpriseNumber(ENTERPRISE_NUMBER);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("turns a duplicate enterprise number into a conflict")
    void turnsADuplicateEnterpriseNumberIntoAConflict() {
        // given
        organisations.save(organisation(ENTERPRISE_NUMBER));

        // when
        Throwable thrown = catchThrowable(() -> organisations.save(organisation(ENTERPRISE_NUMBER)));

        // then
        assertThat(thrown)
                .isInstanceOfSatisfying(
                        ConflictException.class,
                        conflict -> assertThat(conflict.code()).isEqualTo("ORGANISATION_ALREADY_REGISTERED"));
    }

    private static Organisation organisation(EnterpriseNumber enterpriseNumber) {
        return Organisation.restore(
                OrganisationId.generate(),
                enterpriseNumber,
                "Buurtwerk vzw",
                LegalForm.IVZW_AISBL,
                Instant.parse("2026-01-10T09:00:00Z"));
    }
}
