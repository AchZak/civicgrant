package be.achzak.civicgrant.wiring;

import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;
import jakarta.servlet.MultipartConfigElement;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

@WiringTest
@DisplayName("Web server wiring")
class WebServerWiringTest {

    @Autowired
    private MultipartConfigElement multipartConfig;

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("accepts uploads up to the domain's 10 MiB per file and 11 MiB per request")
    void acceptsUploadsUpToTheDomainsLimit() {
        // when
        long[] limits = {multipartConfig.getMaxFileSize(), multipartConfig.getMaxRequestSize()};

        // then
        assertThat(limits).containsExactly(SupportingDocument.MAX_SIZE_IN_BYTES, 11L * 1024 * 1024);
    }

    @Test
    @DisplayName("builds absolute links with the scheme and host forwarded by the proxy")
    void buildsAbsoluteLinksWithTheSchemeAndHostForwardedByTheProxy() throws IOException, InterruptedException {
        // given
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/.well-known/oauth-protected-resource"))
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "civicgrant.example")
                .build();

        // when
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, BodyHandlers.ofString());
        }

        // then
        assertThat(response.body()).contains("\"resource\":\"https://civicgrant.example\"");
    }
}
