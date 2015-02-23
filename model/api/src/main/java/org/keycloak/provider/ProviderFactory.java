package org.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ProviderFactory<T extends Provider> {

    public T create(KeycloakSession session);

    public void init(Config.Scope config);

    /**
     * Called after all provider factories have been initialized
     */
    public void postInit(KeycloakSessionFactory factory);

    public void close();

    public String getId();

}
