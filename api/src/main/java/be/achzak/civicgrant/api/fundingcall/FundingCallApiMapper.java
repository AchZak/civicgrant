package be.achzak.civicgrant.api.fundingcall;

import be.achzak.civicgrant.api.generated.model.FundingCallCollection;
import be.achzak.civicgrant.api.generated.model.OpenFundingCallRequest;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.SubmissionPeriod;
import be.achzak.civicgrant.domain.fundingcall.port.in.OpenFundingCallCommand;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.domain.shared.Money;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

final class FundingCallApiMapper {

    private FundingCallApiMapper() {}

    static OpenFundingCallCommand toCommand(OpenFundingCallRequest request, Actor actor) {
        return new OpenFundingCallCommand(
                request.getReference(),
                new LocalizedText(request.getTitle().getNl(), request.getTitle().getFr()),
                new SubmissionPeriod(
                        request.getOpensAt().toInstant(), request.getClosesAt().toInstant()),
                new Money(request.getTotalBudget()),
                new Money(request.getMaxAmountPerApplication()),
                actor);
    }

    static FundingCallCollection toCollection(List<FundingCall> fundingCalls) {
        return new FundingCallCollection(
                fundingCalls.stream().map(FundingCallApiMapper::toResource).toList());
    }

    static be.achzak.civicgrant.api.generated.model.FundingCall toResource(FundingCall fundingCall) {
        return new be.achzak.civicgrant.api.generated.model.FundingCall(
                fundingCall.id().value(),
                fundingCall.reference(),
                toResource(fundingCall.title()),
                utc(fundingCall.submissionPeriod().opensAt()),
                utc(fundingCall.submissionPeriod().closesAt()),
                fundingCall.budget().capPerApplication().amount(),
                fundingCall.budget().remaining().amount());
    }

    private static be.achzak.civicgrant.api.generated.model.LocalizedText toResource(LocalizedText text) {
        return new be.achzak.civicgrant.api.generated.model.LocalizedText(text.nl(), text.fr());
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
