package be.achzak.civicgrant.domain.grantapplication.port.in;

public interface RejectApplicationUseCase {

    void reject(RejectApplicationCommand command);
}
