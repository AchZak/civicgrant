package be.achzak.civicgrant.api.security;

import io.github.belgif.rest.problem.InvalidAccessTokenProblem;
import io.github.belgif.rest.problem.NoAccessTokenProblem;
import io.github.belgif.rest.problem.api.Problem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

// Hands the problem to Spring MVC, so the Belgif library renders it like every other error.
final class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver handlerExceptionResolver;

    ProblemAuthenticationEntryPoint(HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        Problem problem = exception instanceof OAuth2AuthenticationException
                ? new InvalidAccessTokenProblem()
                : new NoAccessTokenProblem();
        handlerExceptionResolver.resolveException(request, response, null, problem);
    }
}
