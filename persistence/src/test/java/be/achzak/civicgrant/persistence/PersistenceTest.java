package be.achzak.civicgrant.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

// The JPA slice skips @Component classes, so the adapters are added back; one shared context means one container.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest(includeFilters = @ComponentScan.Filter(Component.class))
@Import(TestcontainersConfiguration.class)
public @interface PersistenceTest {}
