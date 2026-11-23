package be.achzak.civicgrant.api.fundingcall;

import static java.time.ZoneOffset.UTC;

import be.achzak.civicgrant.api.generated.model.FundingCallCollectionDto;
import be.achzak.civicgrant.api.generated.model.FundingCallDto;
import be.achzak.civicgrant.api.generated.model.LocalizedTextDto;
import be.achzak.civicgrant.api.generated.model.OpenFundingCallRequestDto;
import be.achzak.civicgrant.core.common.domain.LocalizedText;
import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.fundingcall.application.port.in.OpenFundingCallCommand;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;
import java.util.List;

final class FundingCallApiMapper {

    private FundingCallApiMapper() {}

    static OpenFundingCallCommand toCommand(OpenFundingCallRequestDto request, Actor actor) {
        return new OpenFundingCallCommand(
                FundingCallReference.parse(request.getReference()),
                new LocalizedText(request.getTitle().getNl(), request.getTitle().getFr()),
                new SubmissionPeriod(
                        request.getOpensAt().toInstant(), request.getClosesAt().toInstant()),
                new Money(request.getTotalBudget()),
                new Money(request.getMaxAmountPerApplication()),
                actor);
    }

    static FundingCallCollectionDto toDto(List<FundingCall> fundingCalls) {
        return new FundingCallCollectionDto(
                fundingCalls.stream().map(FundingCallApiMapper::toDto).toList());
    }

    static FundingCallDto toDto(FundingCall fundingCall) {
        return new FundingCallDto(
                fundingCall.id().value(),
                fundingCall.reference().value(),
                toDto(fundingCall.title()),
                fundingCall.submissionPeriod().opensAt().atOffset(UTC),
                fundingCall.submissionPeriod().closesAt().atOffset(UTC),
                fundingCall.budget().capPerApplication().amount(),
                fundingCall.budget().remaining().amount());
    }

    private static LocalizedTextDto toDto(LocalizedText text) {
        return new LocalizedTextDto(text.nl(), text.fr());
    }
}
