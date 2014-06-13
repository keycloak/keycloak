package org.keycloak.models.cache;

import org.keycloak.Config;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class NoCacheKeycloakSessionFactory implements CacheKeycloakSessionFactory {
    @Override
    public CacheKeycloakSession create(ProviderSession providerSession) {
        return new NoCacheKeycloakSession(providerSession);
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void init(Config.Scope config) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getId() {
        return "none";
    }
}
