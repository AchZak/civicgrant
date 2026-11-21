package be.achzak.civicgrant.domain.fundingcall.port.in;

import be.achzak.civicgrant.domain.fundingcall.model.SubmissionPeriod;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.domain.shared.Money;
import java.util.Objects;

public record OpenFundingCallCommand(
        String reference,
        LocalizedText title,
        SubmissionPeriod submissionPeriod,
        Money totalBudget,
        Money capPerApplication,
        Actor actor) {

    public OpenFundingCallCommand {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(submissionPeriod, "submissionPeriod");
        Objects.requireNonNull(totalBudget, "totalBudget");
        Objects.requireNonNull(capPerApplication, "capPerApplication");
        Objects.requireNonNull(actor, "actor");
    }
}
