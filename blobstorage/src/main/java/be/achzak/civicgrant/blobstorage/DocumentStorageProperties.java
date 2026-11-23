package be.achzak.civicgrant.blobstorage;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Either an emulator connection string (local, tests) or an endpoint with the identity's client id (Azure).
@ConfigurationProperties("civicgrant.documents")
record DocumentStorageProperties(
        @Nullable String endpoint,
        String container,
        @Nullable String clientId,
        @Nullable String connectionString) {

    DocumentStorageProperties {
        if (isBlank(container)) {
            throw new IllegalStateException("civicgrant.documents.container must be set");
        }
        if (isBlank(connectionString) && (isBlank(endpoint) || isBlank(clientId))) {
            throw new IllegalStateException(
                    "Set civicgrant.documents.endpoint and client-id (Azure), or connection-string (emulator)");
        }
    }

    boolean usesEmulator() {
        return !isBlank(connectionString);
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
