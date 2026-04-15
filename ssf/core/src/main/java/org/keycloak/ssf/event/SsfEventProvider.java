package org.keycloak.ssf.event;

import org.keycloak.provider.Provider;

/**
 * Per-session provider that exposes the global {@link SsfEventRegistry}.
 *
 * <p>The registry itself is built once at server startup by aggregating the
 * events contributed by every registered {@link SsfEventProviderFactory}, so
 * lookups are cheap and stateless.
 */
public interface SsfEventProvider extends Provider {

    @Override
    default void close() {
    }

    /**
     * @return the global, immutable registry of known SSF events
     */
    SsfEventRegistry getRegistry();
}
