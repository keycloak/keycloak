package org.keycloak.logging;

import java.util.Collection;
import java.util.Collections;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

public final class MappedDiagnosticContextUtil {

    private static final Logger log = Logger.getLogger(MappedDiagnosticContextUtil.class);
    private static final MappedDiagnosticContextProvider NOOP_PROVIDER = new NoopMappedDiagnosticContextProvider();
    private static volatile Collection<String> keysToClear = Collections.emptySet();

    public static MappedDiagnosticContextProvider getMappedDiagnosticContextProvider(KeycloakSession session) {
        if (!Profile.isFeatureEnabled(Profile.Feature.LOG_MDC)) {
            return NOOP_PROVIDER;
        }
        if (session == null) {
            log.warn("Cannot obtain session from thread to init MappedDiagnosticContextProvider. Return Noop provider.");
            return NOOP_PROVIDER;
        }
        MappedDiagnosticContextProvider provider = session.getProvider(MappedDiagnosticContextProvider.class);
        if (provider == null) {
            return NOOP_PROVIDER;
        }
        return provider;
    }

    public static void setKeysToClear(Collection<String> keys) {
        // As the MDC.getMap() clones the context and is possibly expensive, we instead iterate over the list of all known keys
        keysToClear = keys;
    }

    /**
     * Clears the Mapped Diagnostic Context (MDC), but only clears the key/value pairs that were set by this provider.
     */
    public static void clearMdc() {
        for (String key : keysToClear) {
            MDC.remove(key);
        }
    }
}
