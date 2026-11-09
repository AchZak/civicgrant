package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.shared.Money.euros;

import be.achzak.civicgrant.domain.shared.Money;

public final class ProjectProposalBuilder {

    private String title = "Moestuin voor de buurt";
    private String description = "Een gedeelde moestuin op het braakliggende terrein achter het buurthuis.";
    private Money requestedAmount = euros("8000.00");
    private Money totalProjectBudget = euros("12000.00");
    private Language language = Language.NL;

    private ProjectProposalBuilder() {}

    public static ProjectProposalBuilder aProjectProposal() {
        return new ProjectProposalBuilder();
    }

    public ProjectProposalBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public ProjectProposalBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ProjectProposalBuilder withRequestedAmount(Money requestedAmount) {
        this.requestedAmount = requestedAmount;
        return this;
    }

    public ProjectProposalBuilder withTotalProjectBudget(Money totalProjectBudget) {
        this.totalProjectBudget = totalProjectBudget;
        return this;
    }

    public ProjectProposalBuilder withLanguage(Language language) {
        this.language = language;
        return this;
    }

    public ProjectProposal build() {
        return new ProjectProposal(title, description, requestedAmount, totalProjectBudget, language);
    }
}
