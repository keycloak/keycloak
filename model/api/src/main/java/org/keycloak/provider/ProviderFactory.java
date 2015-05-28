package org.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * At boot time, keycloak discovers all factories.  For each discovered factory, the init() method is called.  After
 * all factories have been initialized, the postInit() method is called.  close() is called when the server shuts down.
 *
 * Only one instance of a factory exists per server.
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ProviderFactory<T extends Provider> {

    public T create(KeycloakSession session);

    /**
     * Only called once when the factory is first created.  This config is pulled from keycloak_server.json
     *
     * @param config
     */
    public void init(Config.Scope config);

    /**
     * Called after all provider factories have been initialized
     */
    public void postInit(KeycloakSessionFactory factory);

    /**
     * This is called when the server shuts down.
     *
     */
    public void close();

    public String getId();

}
