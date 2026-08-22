package org.keycloak.it.provider.annotation;

import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.provider.KeycloakProvider;

/**
 * Invalid {@link KeycloakProvider} target: does not implement the {@code EventListenerProviderFactory}
 * it claims to be registered for, so the build-time validation must reject it.
 */
@KeycloakProvider(EventListenerProviderFactory.class)
public class NotAProviderFactory {
}
