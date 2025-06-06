package org.keycloak.logging;

import org.keycloak.models.KeycloakContext;
import org.keycloak.provider.Provider;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
public interface MdcDefinitionProvider extends Provider {

    /**
     * Defines the set of MDC keys that may have potentially been set by {@link #getMdcValues(KeycloakContext)}. Required
     * for resetting MDC values after the request.
     *
     * @return the set of mdc keys
     */
    Set<String> getMdcKeys();

    /**
     * Defines the key/value pairs to set in MDC for given keycloak context.
     *
     * @param keycloakContext the current keycloak context, never null
     * @return key/value pairs to set in MDC
     */
    Map<String, String> getMdcValues(KeycloakContext keycloakContext);
}
