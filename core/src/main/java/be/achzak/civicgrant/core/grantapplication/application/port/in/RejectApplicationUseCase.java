package be.achzak.civicgrant.core.grantapplication.application.port.in;

public interface RejectApplicationUseCase {

    void reject(RejectApplicationCommand command);
}
