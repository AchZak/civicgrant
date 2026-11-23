package be.achzak.civicgrant.blobstorage;

import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AzureBlobStorageProperties.class)
class AzureBlobStorageConfiguration {

    @Bean
    BlobContainerClient documentsContainer(AzureBlobStorageProperties properties) {
        return properties.usesEmulator() ? onEmulator(properties) : onAzure(properties);
    }

    private static BlobContainerClient onAzure(AzureBlobStorageProperties properties) {
        return new BlobServiceClientBuilder()
                .endpoint(properties.endpoint())
                .credential(new ManagedIdentityCredentialBuilder()
                        .clientId(properties.clientId())
                        .build())
                .buildClient()
                .getBlobContainerClient(properties.container());
    }

    private static BlobContainerClient onEmulator(AzureBlobStorageProperties properties) {
        BlobContainerClient container = new BlobServiceClientBuilder()
                .connectionString(properties.connectionString())
                .buildClient()
                .getBlobContainerClient(properties.container());
        // The emulator starts empty. On Azure, Bicep creates the container and the app has no right to.
        container.createIfNotExists();
        return container;
    }
}
