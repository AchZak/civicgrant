package be.achzak.civicgrant.domain.shared;

import org.assertj.core.api.AbstractThrowableAssert;

public final class DomainExceptionAssert extends AbstractThrowableAssert<DomainExceptionAssert, Throwable> {

    private DomainExceptionAssert(Throwable actual) {
        super(actual, DomainExceptionAssert.class);
    }

    public static DomainExceptionAssert assertThatDomainException(Throwable actual) {
        return new DomainExceptionAssert(actual).isInstanceOf(DomainException.class);
    }

    public DomainExceptionAssert isNotFound() {
        return isInstanceOf(NotFoundException.class);
    }

    public DomainExceptionAssert isForbidden() {
        return isInstanceOf(ForbiddenException.class);
    }

    public DomainExceptionAssert isBusinessRuleViolation() {
        return isInstanceOf(BusinessRuleViolationException.class);
    }

    public DomainExceptionAssert isConflict() {
        return isInstanceOf(ConflictException.class);
    }

    public DomainExceptionAssert hasCode(String expectedCode) {
        String actualCode = ((DomainException) actual).code();
        if (!actualCode.equals(expectedCode)) {
            failWithActualExpectedAndMessage(
                    actualCode, expectedCode, "Expecting code <%s> but was <%s>", expectedCode, actualCode);
        }
        return this;
    }
}
