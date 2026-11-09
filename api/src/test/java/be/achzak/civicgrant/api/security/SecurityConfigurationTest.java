package be.achzak.civicgrant.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;

@DisplayName("Security configuration")
class SecurityConfigurationTest {

    private final OAuth2ResourceServerProperties properties = new OAuth2ResourceServerProperties();

    @Test
    @DisplayName("refuses to start without a token issuer")
    void refusesToStartWithoutAnIssuer() {
        // given
        properties.getJwt().setAudiences(List.of("civicgrant-api-test"));

        // when
        Throwable thrown = catchThrowable(() -> new SecurityConfiguration(properties));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("issuer-uri");
    }

    @Test
    @DisplayName("refuses to start without a token audience")
    void refusesToStartWithoutAnAudience() {
        // given
        properties.getJwt().setIssuerUri("https://issuer.test/v2.0");

        // when
        Throwable thrown = catchThrowable(() -> new SecurityConfiguration(properties));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("audiences");
    }

    @Test
    @DisplayName("starts with a token issuer and audience")
    void startsWithAnIssuerAndAudience() {
        // given
        properties.getJwt().setIssuerUri("https://issuer.test/v2.0");
        properties.getJwt().setAudiences(List.of("civicgrant-api-test"));

        // when
        Throwable thrown = catchThrowable(() -> new SecurityConfiguration(properties));

        // then
        assertThat(thrown).isNull();
    }
}
