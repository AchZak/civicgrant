package be.achzak.civicgrant.persistence.fundingcall;

import be.achzak.civicgrant.domain.fundingcall.model.BudgetEnvelope;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.model.SubmissionPeriod;
import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.domain.shared.Money;

final class FundingCallJpaMapper {

    private FundingCallJpaMapper() {}

    static FundingCall toDomain(FundingCallJpaEntity entity) {
        return FundingCall.restore(
                new FundingCallId(entity.getId()),
                entity.getReference(),
                new LocalizedText(entity.getTitleNl(), entity.getTitleFr()),
                new SubmissionPeriod(entity.getOpensAt(), entity.getClosesAt()),
                new BudgetEnvelope(
                        new Money(entity.getTotalBudget()),
                        new Money(entity.getCommittedAmount()),
                        new Money(entity.getMaxAmountPerApplication())),
                entity.getVersion(),
                entity.getCreatedAt());
    }
}
