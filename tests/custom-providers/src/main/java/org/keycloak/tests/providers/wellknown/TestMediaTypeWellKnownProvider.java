package org.keycloak.tests.providers.wellknown;

import org.keycloak.wellknown.WellKnownProvider;

public class TestMediaTypeWellKnownProvider implements WellKnownProvider {

    public static final String CONFIG = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJ0ZXN0In0.";
    public static final String MEDIA_TYPE = "application/entity-statement+jwt";

    @Override
    public Object getConfig() {
        return CONFIG;
    }

    @Override
    public String getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public void close() {
    }
}
