package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Command;
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
