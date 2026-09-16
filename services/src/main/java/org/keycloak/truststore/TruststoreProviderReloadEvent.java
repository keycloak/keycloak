package org.keycloak.truststore;

import org.keycloak.provider.ProviderEvent;

/**
 * An event fired when the system truststore was reloaded due to changes in trusted certificates.
 * This event is only intended for {@link TruststoreProvider}s. If you need to update your integration
 * when the system truststore has changed, observe {@link TruststoreReloadEvent} instead.
 */
public final class TruststoreProviderReloadEvent implements ProviderEvent {

    private static final TruststoreProviderReloadEvent INSTANCE = new TruststoreProviderReloadEvent();

    private TruststoreProviderReloadEvent() {
    }

    public static ProviderEvent get() {
        return INSTANCE;
    }

    public static boolean isTruststoreProviderReloadEvent(ProviderEvent event) {
        return INSTANCE == event;
    }
}
