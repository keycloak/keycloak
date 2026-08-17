package org.keycloak.it.provider.annotation;

import org.keycloak.provider.KeycloakProvider;

/**
 * Invalid {@link KeycloakProvider} target: does not implement {@code ProviderFactory}, so the
 * build-time validation must reject it.
 */
@KeycloakProvider
public class NotAProviderFactory {
}
