package be.achzak.civicgrant.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
    }

    @Bean
    AzuriteContainer azuriteContainer() {
        return new AzuriteContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.37.0")) {
            @Override
            protected void configure() {
                super.configure();
                // The SDK speaks Blob API 2026-10-06, newer than the newest Azurite release knows.
                withCommand(String.join(" ", getCommandParts()) + " --skipApiVersionCheck");
            }
        };
    }

    @Bean
    DynamicPropertyRegistrar documentStorageProperties(AzuriteContainer azurite) {
        return properties -> {
            properties.add("civicgrant.documents.connection-string", azurite::getConnectionString);
            properties.add("civicgrant.documents.container", () -> "documents-test");
        };
    }
}
