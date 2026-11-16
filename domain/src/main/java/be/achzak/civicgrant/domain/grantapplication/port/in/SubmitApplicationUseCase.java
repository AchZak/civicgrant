package be.achzak.civicgrant.domain.grantapplication.port.in;

public interface SubmitApplicationUseCase {

    void submit(SubmitApplicationCommand command);
}
