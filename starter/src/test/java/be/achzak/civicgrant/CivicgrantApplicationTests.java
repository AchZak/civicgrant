package be.achzak.civicgrant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test/v2.0",
            "spring.security.oauth2.resourceserver.jwt.audiences=civicgrant-api-test"
        })
@DisplayName("CivicGrant application")
class CivicgrantApplicationTests {

    @Test
    @DisplayName("starts the full context against PostgreSQL")
    void contextLoads() {}
}
