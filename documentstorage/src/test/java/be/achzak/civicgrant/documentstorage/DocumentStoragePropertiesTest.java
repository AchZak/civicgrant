package be.achzak.civicgrant.documentstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Document storage settings")
class DocumentStoragePropertiesTest {

    private static final String ENDPOINT = "https://stcivicgrant.blob.core.windows.net/";
    private static final String CLIENT_ID = "78194fe8-832d-423d-9a6e-45f08e81a503";

    @Test
    @DisplayName("accepts an endpoint with the identity's client id, for Azure")
    void acceptsAnEndpointWithTheIdentitysClientId() {
        // when
        DocumentStorageProperties properties =
                new DocumentStorageProperties(ENDPOINT, "documents-tst", CLIENT_ID, null);

        // then
        assertThat(properties.usesEmulator()).isFalse();
    }

    @Test
    @DisplayName("accepts a connection string, for the emulator")
    void acceptsAConnectionString() {
        // when
        DocumentStorageProperties properties =
                new DocumentStorageProperties(null, "documents-local", null, "UseDevelopmentStorage=true");

        // then
        assertThat(properties.usesEmulator()).isTrue();
    }

    @Test
    @DisplayName("refuses to start without a container")
    void refusesToStartWithoutAContainer() {
        // when
        Throwable thrown = catchThrowable(() -> new DocumentStorageProperties(ENDPOINT, " ", CLIENT_ID, null));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("container");
    }

    @Test
    @DisplayName("refuses an endpoint without the identity's client id")
    void refusesAnEndpointWithoutTheIdentitysClientId() {
        // when
        Throwable thrown = catchThrowable(() -> new DocumentStorageProperties(ENDPOINT, "documents-tst", null, null));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("client-id");
    }
}
