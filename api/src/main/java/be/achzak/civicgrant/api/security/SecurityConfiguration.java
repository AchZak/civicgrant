package be.achzak.civicgrant.api.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
class SecurityConfiguration {

    private static final String PROPERTY_PREFIX = "spring.security.oauth2.resourceserver.jwt.";

    // Spring Boot silently skips the issuer or audience check when its property is missing.
    SecurityConfiguration(OAuth2ResourceServerProperties properties) {
        OAuth2ResourceServerProperties.Jwt jwt = properties.getJwt();
        if (!StringUtils.hasText(jwt.getIssuerUri())) {
            throw new IllegalStateException(PROPERTY_PREFIX + "issuer-uri is not set");
        }
        if (jwt.getAudiences().isEmpty()) {
            throw new IllegalStateException(PROPERTY_PREFIX + "audiences is not set");
        }
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver)
            throws Exception {
        ProblemAuthenticationEntryPoint entryPoint = new ProblemAuthenticationEntryPoint(handlerExceptionResolver);
        return http.authorizeHttpRequests(
                        requests -> requests.requestMatchers("/actuator/health", "/actuator/health/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(entraConverter()))
                        .authenticationEntryPoint(entryPoint))
                // The resource server's default only covers API-like requests; a browser asking for HTML would fall
                // through.
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    OAuth2TokenValidator<Jwt> userAccessTokenValidator() {
        return new UserAccessTokenValidator();
    }

    private static JwtAuthenticationConverter entraConverter() {
        JwtGrantedAuthoritiesConverter appRoles = new JwtGrantedAuthoritiesConverter();
        appRoles.setAuthoritiesClaimName(EntraAccessToken.APP_ROLES_CLAIM);
        appRoles.setAuthorityPrefix(EntraAccessToken.ROLE_AUTHORITY_PREFIX);

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(appRoles);
        converter.setPrincipalClaimName(EntraAccessToken.OBJECT_ID_CLAIM);
        return converter;
    }
}
