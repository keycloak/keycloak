package org.keycloak.logging;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.keycloak.models.KeycloakSession;

import java.util.ArrayList;

public final class MappedDiagnosticContextUtil {

    private static final Logger log = Logger.getLogger(MappedDiagnosticContextUtil.class);
    private static final MappedDiagnosticContextProvider NOOP_PROVIDER = new NoopMappedDiagnosticContextProvider();

    public static MappedDiagnosticContextProvider getMappedDiagnosticContextProvider(KeycloakSession session) {
        if (session == null) {
            log.warn("Cannot obtain session from thread to init MappedDiagnosticContextProvider. Return Noop provider.");
            return NOOP_PROVIDER;
        }
        return session.getProvider(MappedDiagnosticContextProvider.class);
    }

    /**
     * Clears the Mapped Diagnostic Context (MDC), but only clears the key/value pairs that were set by this provider.
     */
    public static void clearMdc() {
        for (String key : new ArrayList<>(MDC.getMap().keySet())) {
            if (key.startsWith(MappedDiagnosticContextProvider.MDC_PREFIX)) {
                MDC.remove(key);
            }
        }
    }
}
