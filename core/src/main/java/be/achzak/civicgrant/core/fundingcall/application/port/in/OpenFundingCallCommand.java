package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.LocalizedText;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import java.util.Objects;

public record OpenFundingCallCommand(
        FundingCallReference reference,
        LocalizedText title,
        SubmissionPeriod submissionPeriod,
        Money totalBudget,
        Money capPerApplication,
        Actor actor)
        implements Command {

    public OpenFundingCallCommand {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(submissionPeriod, "submissionPeriod");
        Objects.requireNonNull(totalBudget, "totalBudget");
        Objects.requireNonNull(capPerApplication, "capPerApplication");
        Objects.requireNonNull(actor, "actor");
    }
}
