package be.achzak.civicgrant;

import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.domain.fundingcall.service.ListOpenFundingCallsService;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.domain.organisation.service.RegisterOrganisationService;
import be.achzak.civicgrant.domain.shared.ConflictException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.interceptor.TransactionAttribute;

@DisplayName("Use case transactions")
class UseCaseTransactionAttributeSourceTest {

    private final UseCaseTransactionAttributeSource source = new UseCaseTransactionAttributeSource();

    @Test
    @DisplayName("runs a command in a read-write transaction")
    void runsACommandInAReadWriteTransaction() throws NoSuchMethodException {
        // given
        Method register = RegisterOrganisationService.class.getMethod("register", RegisterOrganisationCommand.class);

        // when
        TransactionAttribute attribute = source.getTransactionAttribute(register, RegisterOrganisationService.class);

        // then
        assertThat(attribute).isNotNull();
        assertThat(attribute.isReadOnly()).isFalse();
    }

    @Test
    @DisplayName("rolls a command back when the domain refuses it")
    void rollsACommandBackWhenTheDomainRefusesIt() throws NoSuchMethodException {
        // given
        Method register = RegisterOrganisationService.class.getMethod("register", RegisterOrganisationCommand.class);
        TransactionAttribute attribute = source.getTransactionAttribute(register, RegisterOrganisationService.class);

        // when
        boolean rollsBack = attribute.rollbackOn(new ConflictException("ORGANISATION_ALREADY_REGISTERED", "Duplicate"));

        // then
        assertThat(rollsBack).isTrue();
    }

    @Test
    @DisplayName("runs a query in a read-only transaction")
    void runsAQueryInAReadOnlyTransaction() throws NoSuchMethodException {
        // given
        Method list = ListOpenFundingCallsService.class.getMethod("list", ListOpenFundingCallsQuery.class);

        // when
        TransactionAttribute attribute = source.getTransactionAttribute(list, ListOpenFundingCallsService.class);

        // then
        assertThat(attribute).isNotNull();
        assertThat(attribute.isReadOnly()).isTrue();
    }

    @Test
    @DisplayName("leaves a service's other methods without a transaction")
    void leavesAServicesOtherMethodsWithoutATransaction() throws NoSuchMethodException {
        // given
        Method equals = RegisterOrganisationService.class.getMethod("equals", Object.class);

        // when
        TransactionAttribute attribute = source.getTransactionAttribute(equals, RegisterOrganisationService.class);

        // then
        assertThat(attribute).isNull();
    }
}
