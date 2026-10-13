package org.keycloak.encoding;

import java.io.IOException;
import java.io.InputStream;

import org.keycloak.provider.Provider;

public interface ResourceEncodingProvider extends Provider {

    InputStream getEncodedStream(StreamSupplier producer, String... path);

    String getEncoding();

    @Override
    default void close() {
    }

    interface StreamSupplier {

        InputStream getInputStream() throws IOException;

    }

}
