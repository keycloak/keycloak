package org.keycloak.compatibility;

import java.util.Map;
import org.keycloak.common.Version;

/**
 * A {@link CompatibilityMetadataProvider} implementation to provide the Keycloak version.
 */
public class KeycloakCompatibilityMetadataProvider implements CompatibilityMetadataProvider {

    public static final String ID = "keycloak";

    @Override
    public Map<String, String> metadata() {
        return Map.of("version", Version.VERSION);
    }

    @Override
    public String getId() {
        return "keycloak";
    }
}
