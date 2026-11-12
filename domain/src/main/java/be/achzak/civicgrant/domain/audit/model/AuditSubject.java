package be.achzak.civicgrant.domain.audit.model;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import java.util.Objects;

public sealed interface AuditSubject {

    record Application(ApplicationId id) implements AuditSubject {

        public Application {
            Objects.requireNonNull(id, "id");
        }
    }

    record Organisation(OrganisationId id) implements AuditSubject {

        public Organisation {
            Objects.requireNonNull(id, "id");
        }
    }
}
