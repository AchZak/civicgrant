package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.common.application.cqs.Command;
import be.achzak.civicgrant.core.common.domain.LocalizedText;
import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;

public record OpenFundingCallCommand(
        FundingCallReference reference,
        LocalizedText title,
        SubmissionPeriod submissionPeriod,
        Money totalBudget,
        Money capPerApplication,
        Actor actor)
        implements Command {}
