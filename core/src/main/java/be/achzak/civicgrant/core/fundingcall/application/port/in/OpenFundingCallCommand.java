package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.LocalizedText;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;

public record OpenFundingCallCommand(
        FundingCallReference reference,
        LocalizedText title,
        SubmissionPeriod submissionPeriod,
        Money totalBudget,
        Money capPerApplication,
        Actor actor)
        implements Command {}
