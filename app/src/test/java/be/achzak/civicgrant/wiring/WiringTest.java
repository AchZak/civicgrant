package be.achzak.civicgrant.wiring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// Classes using it share one context and its containers, as long as none adds beans or properties of its own.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test/v2.0",
            "spring.security.oauth2.resourceserver.jwt.audiences=civicgrant-api-test"
        })
@Import({TestcontainersConfiguration.class, WiringFixtures.class})
@interface WiringTest {}
