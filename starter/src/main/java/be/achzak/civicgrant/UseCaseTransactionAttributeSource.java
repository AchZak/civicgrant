package be.achzak.civicgrant;

import java.lang.reflect.Method;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

final class UseCaseTransactionAttributeSource implements TransactionAttributeSource {

    private static final TransactionAttribute READ_WRITE = new DefaultTransactionAttribute();
    private static final TransactionAttribute READ_ONLY = readOnly();

    @Override
    public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
        if (targetClass == null || !isDomainService(targetClass) || method.getParameterCount() != 1) {
            return null;
        }
        String parameterType = method.getParameterTypes()[0].getSimpleName();
        if (parameterType.endsWith("Command")) {
            return READ_WRITE;
        }
        if (parameterType.endsWith("Query")) {
            return READ_ONLY;
        }
        return null;
    }

    private static boolean isDomainService(Class<?> type) {
        String packageName = type.getPackageName();
        return packageName.startsWith("be.achzak.civicgrant.domain.") && packageName.endsWith(".service");
    }

    private static TransactionAttribute readOnly() {
        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setReadOnly(true);
        return attribute;
    }
}
