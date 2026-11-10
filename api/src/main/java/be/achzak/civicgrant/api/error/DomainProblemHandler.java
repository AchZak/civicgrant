package be.achzak.civicgrant.api.error;

import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import be.achzak.civicgrant.domain.shared.ConflictException;
import be.achzak.civicgrant.domain.shared.DomainException;
import be.achzak.civicgrant.domain.shared.ForbiddenException;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import io.github.belgif.rest.problem.DefaultProblem;
import io.github.belgif.rest.problem.ResourceNotFoundProblem;
import io.github.belgif.rest.problem.api.Problem;
import io.github.belgif.rest.problem.spring.ProblemMediaType;
import java.net.URI;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Runs before the Belgif library's advice, whose catch-all would answer every domain exception with a 500.
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class DomainProblemHandler {

    private static final String TYPE_PREFIX = "urn:problem-type:civicgrant:";

    @ExceptionHandler(DomainException.class)
    ResponseEntity<Problem> handle(DomainException exception) {
        Problem problem = switch (exception) {
            case NotFoundException _ -> new ResourceNotFoundProblem();
            case BusinessRuleViolationException _ -> businessProblem(exception.code(), HttpStatus.BAD_REQUEST);
            case ForbiddenException _ -> businessProblem(exception.code(), HttpStatus.FORBIDDEN);
            case ConflictException _ -> businessProblem(exception.code(), HttpStatus.CONFLICT);
        };
        problem.setDetail(exception.getMessage());
        return ProblemMediaType.INSTANCE.toResponse(problem);
    }

    private static Problem businessProblem(String code, HttpStatus status) {
        return new DefaultProblem(URI.create(TYPE_PREFIX + lowerCamelCase(code)), null, title(code), status.value());
    }

    private static String lowerCamelCase(String code) {
        String[] words = code.toLowerCase(Locale.ROOT).split("_");
        StringBuilder name = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            name.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
        }
        return name.toString();
    }

    private static String title(String code) {
        String words = code.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
