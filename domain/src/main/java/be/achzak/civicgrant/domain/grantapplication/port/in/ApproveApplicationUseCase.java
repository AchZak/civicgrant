package be.achzak.civicgrant.domain.grantapplication.port.in;

public interface ApproveApplicationUseCase {

    void approve(ApproveApplicationCommand command);
}
