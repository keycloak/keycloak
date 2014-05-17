package org.keycloak.models;

import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface KeycloakSessionFactory extends ProviderFactory<KeycloakSession> {
    KeycloakSession create(ProviderSession providerSession);
    void close();
}
