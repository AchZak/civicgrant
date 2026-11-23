package be.achzak.civicgrant.persistence.fundingcall;

import be.achzak.civicgrant.core.common.domain.LocalizedText;
import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.fundingcall.domain.BudgetEnvelope;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;

final class FundingCallJpaMapper {

    private FundingCallJpaMapper() {}

    static FundingCall toDomain(FundingCallJpaEntity entity) {
        return FundingCall.restore(
                new FundingCallId(entity.getId()),
                new FundingCallReference(entity.getReference()),
                new LocalizedText(entity.getTitleNl(), entity.getTitleFr()),
                new SubmissionPeriod(entity.getOpensAt(), entity.getClosesAt()),
                new BudgetEnvelope(
                        new Money(entity.getTotalBudget()),
                        new Money(entity.getCommittedAmount()),
                        new Money(entity.getMaxAmountPerApplication())),
                entity.getVersion(),
                entity.getCreatedAt());
    }

    static FundingCallJpaEntity toEntity(FundingCall fundingCall) {
        return FundingCallJpaEntity.builder()
                .id(fundingCall.id().value())
                .reference(fundingCall.reference().value())
                .titleNl(fundingCall.title().nl())
                .titleFr(fundingCall.title().fr())
                .opensAt(fundingCall.submissionPeriod().opensAt())
                .closesAt(fundingCall.submissionPeriod().closesAt())
                .totalBudget(fundingCall.budget().total().amount())
                .committedAmount(fundingCall.budget().committed().amount())
                .maxAmountPerApplication(
                        fundingCall.budget().capPerApplication().amount())
                .createdAt(fundingCall.createdAt())
                .build();
    }

    static void update(FundingCallJpaEntity entity, FundingCall fundingCall) {
        entity.setCommittedAmount(fundingCall.budget().committed().amount());
    }
}
