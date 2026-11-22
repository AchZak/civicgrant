package be.achzak.civicgrant.core.grantapplication.application.port.out;

public final class InMemoryApplicationReferenceSequence implements ApplicationReferenceSequence {

    private long last;

    @Override
    public long nextNumber() {
        return ++last;
    }
}
