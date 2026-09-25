package org.keycloak.models.untrustedlogin;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

public interface UntrustedLoginProviderFactory extends ProviderFactory<UntrustedLoginProvider> {
    @Override
    UntrustedLoginProvider create(KeycloakSession session);
}