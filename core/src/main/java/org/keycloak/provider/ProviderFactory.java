package org.keycloak.provider;

import org.keycloak.Config;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ProviderFactory<T extends Provider> {

    public T create(ProviderSession providerSession);

    public void init(Config.Scope config);

    public void close();

    public String getId();

}
