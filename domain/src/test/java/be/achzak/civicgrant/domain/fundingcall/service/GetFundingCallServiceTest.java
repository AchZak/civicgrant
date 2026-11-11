package be.achzak.civicgrant.domain.fundingcall.service;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.domain.fundingcall.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.domain.shared.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Getting a funding call")
class GetFundingCallServiceTest {

    @Test
    @DisplayName("returns the funding call with this id")
    void returnsTheFundingCallWithThisId() {
        // given
        FundingCall fundingCall = aFundingCall().build();
        GetFundingCallService service = new GetFundingCallService(
                new InMemoryFundingCallRepository(aFundingCall().build(), fundingCall));

        // when
        FundingCall found = service.get(new GetFundingCallQuery(fundingCall.id(), anApplicant()));

        // then
        assertThat(found).isSameAs(fundingCall);
    }

    @Test
    @DisplayName("refuses an unknown funding call as not found")
    void refusesUnknownFundingCallAsNotFound() {
        // given
        GetFundingCallService service = new GetFundingCallService(new InMemoryFundingCallRepository());

        // when
        Throwable thrown =
                catchThrowable(() -> service.get(new GetFundingCallQuery(FundingCallId.generate(), anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("FUNDING_CALL_NOT_FOUND");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @DisplayName("lets every role get a funding call")
    void letsEveryRoleGetAFundingCall(Role role) {
        // given
        FundingCall fundingCall = aFundingCall().build();
        GetFundingCallService service = new GetFundingCallService(new InMemoryFundingCallRepository(fundingCall));

        // when
        FundingCall found = service.get(new GetFundingCallQuery(fundingCall.id(), actor("user-1", role)));

        // then
        assertThat(found).isSameAs(fundingCall);
    }

    @Test
    @DisplayName("refuses an actor without any role")
    void refusesActorWithoutAnyRole() {
        // given
        FundingCall fundingCall = aFundingCall().build();
        GetFundingCallService service = new GetFundingCallService(new InMemoryFundingCallRepository(fundingCall));

        // when
        Throwable thrown =
                catchThrowable(() -> service.get(new GetFundingCallQuery(fundingCall.id(), anActorWithoutRoles())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
