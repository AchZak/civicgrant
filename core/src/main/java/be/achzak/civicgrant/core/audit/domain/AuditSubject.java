package be.achzak.civicgrant.core.audit.domain;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
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

    record FundingCall(FundingCallId id) implements AuditSubject {

        public FundingCall {
            Objects.requireNonNull(id, "id");
        }
    }
}
