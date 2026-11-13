package be.achzak.civicgrant.persistence.grantapplication;

import be.achzak.civicgrant.domain.grantapplication.port.out.ApplicationReferenceSequence;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class ApplicationReferenceSequenceAdapter implements ApplicationReferenceSequence {

    private final JdbcClient jdbc;

    ApplicationReferenceSequenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long nextNumber() {
        return jdbc.sql("SELECT nextval('application_reference_seq')")
                .query(Long.class)
                .single();
    }
}
