package org.keycloak.logging;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;

import java.util.ArrayList;

public final class MappedDiagnosticContextUtil {

    private static final Logger log = Logger.getLogger(MappedDiagnosticContextUtil.class);
    private static final MappedDiagnosticContextProvider NOOP_PROVIDER = new NoopMappedDiagnosticContextProvider();

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

    /**
     * Clears the Mapped Diagnostic Context (MDC), but only clears the key/value pairs that were set by this provider.
     */
    public static void clearMdc() {
        if (Profile.isFeatureEnabled(Profile.Feature.LOG_MDC)) {
            // getMap() is relatively expensive as it actually copies the context, but just calling MDC.clear() is not an option because it might affect otel tracing.
            for (String key : new ArrayList<>(MDC.getMap().keySet())) {
                if (key.startsWith(MappedDiagnosticContextProvider.MDC_PREFIX)) {
                    MDC.remove(key);
                }
            }
        }
    }
}
