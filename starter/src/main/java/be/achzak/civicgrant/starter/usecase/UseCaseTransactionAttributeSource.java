package be.achzak.civicgrant.starter.usecase;

import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import java.lang.reflect.Method;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

final class UseCaseTransactionAttributeSource implements TransactionAttributeSource {

    private static final TransactionAttribute READ_WRITE = new DefaultTransactionAttribute();
    private static final TransactionAttribute READ_ONLY = readOnly();

    @Override
    public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
        if (method.getParameterCount() != 1) {
            return null;
        }
        Class<?> request = method.getParameterTypes()[0];
        if (Command.class.isAssignableFrom(request)) {
            return READ_WRITE;
        }
        if (Query.class.isAssignableFrom(request)) {
            return READ_ONLY;
        }
        return null;
    }

    private static TransactionAttribute readOnly() {
        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setReadOnly(true);
        return attribute;
    }
}
