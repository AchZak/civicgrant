package be.achzak.civicgrant;

import java.time.Clock;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.interceptor.TransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration(proxyBeanMethods = false)
@ComponentScan(
        basePackages = "be.achzak.civicgrant.domain",
        useDefaultFilters = false,
        includeFilters = @Filter(type = FilterType.REGEX, pattern = ".+\\.service\\.[^.]+Service"))
class DomainConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    // Infrastructure role, like Spring's own transaction advisor: every auto-proxy creator then applies it.
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor useCaseTransactionAdvisor(BeanFactory beanFactory) {
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionAttributeSource(new UseCaseTransactionAttributeSource());
        interceptor.setBeanFactory(beanFactory);
        return new TransactionAttributeSourceAdvisor(interceptor);
    }
}
