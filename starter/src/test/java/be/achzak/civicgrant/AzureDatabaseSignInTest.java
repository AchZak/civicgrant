package be.achzak.civicgrant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

// No code references these classes: only the JDBC URL set on the Container App names the plugin.
@DisplayName("Azure database sign-in")
class AzureDatabaseSignInTest {

    @Test
    @DisplayName("packages the authentication plugin the Azure datasource URL names")
    void packagesTheAuthenticationPlugin() {
        // when
        boolean present = isPresent("com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin");

        // then
        assertThat(present).isTrue();
    }

    @Test
    @DisplayName("uses the JDK HTTP client as the only Azure SDK transport")
    void usesTheJdkHttpClientOnly() {
        // when
        boolean jdkClient = isPresent("com.azure.core.http.jdk.httpclient.JdkHttpClientProvider");
        boolean nettyClient = isPresent("com.azure.core.http.netty.NettyAsyncHttpClientProvider");

        // then
        assertThat(jdkClient).isTrue();
        assertThat(nettyClient).isFalse();
    }

    private static boolean isPresent(String className) {
        return ClassUtils.isPresent(className, AzureDatabaseSignInTest.class.getClassLoader());
    }
}
