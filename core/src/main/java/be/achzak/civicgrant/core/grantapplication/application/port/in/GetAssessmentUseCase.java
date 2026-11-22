package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.Assessment;

public interface GetAssessmentUseCase {

    Assessment get(GetAssessmentQuery query);
}
