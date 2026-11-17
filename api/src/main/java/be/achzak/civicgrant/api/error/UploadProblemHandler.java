package be.achzak.civicgrant.api.error;

import io.github.belgif.rest.problem.BadRequestProblem;
import io.github.belgif.rest.problem.PayloadTooLargeProblem;
import io.github.belgif.rest.problem.api.InEnum;
import io.github.belgif.rest.problem.api.InputValidationIssues;
import io.github.belgif.rest.problem.api.Problem;
import io.github.belgif.rest.problem.spring.ProblemMediaType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

// The Belgif library maps neither, so its catch-all would answer both with a 500.
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class UploadProblemHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Problem> handleTooLarge() {
        return ProblemMediaType.INSTANCE.toResponse(new PayloadTooLargeProblem());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<Problem> handleMissingPart(MissingServletRequestPartException exception) {
        return ProblemMediaType.INSTANCE.toResponse(new BadRequestProblem(InputValidationIssues.schemaViolation(
                InEnum.BODY, "/" + exception.getRequestPartName(), null, exception.getMessage())));
    }
}
