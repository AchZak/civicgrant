package be.achzak.civicgrant.domain.grantapplication.port.out;

public final class InMemoryApplicationReferenceSequence implements ApplicationReferenceSequence {

    private long last;

    @Override
    public long nextNumber() {
        return ++last;
    }
}
