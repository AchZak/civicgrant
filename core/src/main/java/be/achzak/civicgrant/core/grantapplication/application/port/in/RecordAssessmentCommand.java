package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record RecordAssessmentCommand(
        ApplicationId applicationId, boolean eligible, Recommendation recommendation, String comment, Actor actor)
        implements Command {

    public RecordAssessmentCommand {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(recommendation, "recommendation");
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(actor, "actor");
    }
}
