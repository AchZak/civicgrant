package be.achzak.civicgrant.persistence.grantapplication;

import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.domain.grantapplication.port.out.ApplicationReferenceSequence;
import be.achzak.civicgrant.persistence.PersistenceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@PersistenceTest
@DisplayName("Application reference sequence")
class ApplicationReferenceSequenceAdapterTest {

    @Autowired
    private ApplicationReferenceSequence sequence;

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("hands out consecutive numbers")
    void handsOutConsecutiveNumbers() {
        // given
        long first = sequence.nextNumber();

        // when
        long second = sequence.nextNumber();

        // then
        assertThat(second).isEqualTo(first + 1);
    }

    @Test
    @DisplayName("stops at the six digits of a reference")
    void stopsAtTheSixDigitsOfAReference() {
        // when
        // Read from the catalog: pushing the sequence to its limit would outlive the test's rollback.
        long maxValue = jdbc.sql("""
                        SELECT max_value FROM pg_sequences
                        WHERE schemaname = 'civicgrant' AND sequencename = 'application_reference_seq'
                        """).query(Long.class).single();

        // then
        assertThat(maxValue).isEqualTo(999_999L);
    }
}
