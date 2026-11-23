package be.achzak.civicgrant.core.organisation.domain;

import static be.achzak.civicgrant.core.organisation.domain.OrganisationErrorCode.ORGANISATION_NAME_REQUIRED;
import static be.achzak.civicgrant.core.organisation.domain.OrganisationErrorCode.ORGANISATION_NAME_TOO_LONG;

import be.achzak.civicgrant.core.common.domain.error.BusinessRuleViolationException;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.organisation.domain.event.OrganisationEvent;
import be.achzak.civicgrant.core.organisation.domain.event.OrganisationRegistered;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Organisation {

    private static final int MAX_NAME_LENGTH = 200;

    private final OrganisationId id;
    private final EnterpriseNumber enterpriseNumber;
    private final String name;
    private final LegalForm legalForm;
    private final Instant createdAt;
    private final List<OrganisationEvent> events = new ArrayList<>();

    private Organisation(
            OrganisationId id, EnterpriseNumber enterpriseNumber, String name, LegalForm legalForm, Instant createdAt) {
        this.id = id;
        this.enterpriseNumber = enterpriseNumber;
        this.name = name;
        this.legalForm = legalForm;
        this.createdAt = createdAt;
    }

    public static Organisation create(
            EnterpriseNumber enterpriseNumber, String name, LegalForm legalForm, Actor registeredBy, Instant now) {
        Organisation organisation =
                new Organisation(OrganisationId.generate(), enterpriseNumber, validName(name), legalForm, now);
        organisation.events.add(new OrganisationRegistered(organisation.id, enterpriseNumber, registeredBy.id(), now));
        return organisation;
    }

    public static Organisation restore(
            OrganisationId id, EnterpriseNumber enterpriseNumber, String name, LegalForm legalForm, Instant createdAt) {
        return new Organisation(id, enterpriseNumber, name, legalForm, createdAt);
    }

    private static String validName(String name) {
        String stripped = name.strip();
        if (stripped.isEmpty()) {
            throw new BusinessRuleViolationException(ORGANISATION_NAME_REQUIRED, "Organisation name is blank");
        }
        if (stripped.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleViolationException(
                    ORGANISATION_NAME_TOO_LONG, "Organisation name exceeds " + MAX_NAME_LENGTH + " characters");
        }
        return stripped;
    }

    public List<OrganisationEvent> pullEvents() {
        List<OrganisationEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public OrganisationId id() {
        return id;
    }

    public EnterpriseNumber enterpriseNumber() {
        return enterpriseNumber;
    }

    public String name() {
        return name;
    }

    public LegalForm legalForm() {
        return legalForm;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
