package org.keycloak.models.cache;

import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface CacheKeycloakSessionFactory extends ProviderFactory<CacheKeycloakSession> {
    CacheKeycloakSession create(ProviderSession providerSession);
    void close();
}
