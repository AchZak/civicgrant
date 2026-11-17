package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.Assessment;

public interface GetAssessmentUseCase {

    Assessment get(GetAssessmentQuery query);
}
