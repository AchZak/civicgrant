package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Arrays;
import java.util.Objects;

public record AttachDocumentCommand(
        ApplicationId applicationId, String fileName, String declaredContentType, byte[] content, Actor actor) {

    public AttachDocumentCommand {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(declaredContentType, "declaredContentType");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(actor, "actor");
    }

    // A record compares arrays by identity, so these compare and print the content instead.
    @Override
    public boolean equals(Object other) {
        return other instanceof AttachDocumentCommand that
                && applicationId.equals(that.applicationId)
                && fileName.equals(that.fileName)
                && declaredContentType.equals(that.declaredContentType)
                && Arrays.equals(content, that.content)
                && actor.equals(that.actor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, fileName, declaredContentType, Arrays.hashCode(content), actor);
    }

    @Override
    public String toString() {
        return "AttachDocumentCommand[applicationId=" + applicationId + ", fileName=" + fileName
                + ", declaredContentType=" + declaredContentType + ", content=" + content.length + " bytes, actor="
                + actor + "]";
    }
}
