package be.achzak.civicgrant.wiring;

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
        return new AzuriteContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.38.0")) {
            @Override
            protected void configure() {
                super.configure();
                // Azurite often learns a Blob API version after the SDK speaks it: SDK updates shouldn't wait.
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
