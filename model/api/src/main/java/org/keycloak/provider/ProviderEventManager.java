package org.keycloak.provider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ProviderEventManager {
    void register(ProviderEventListener listener);

    void unregister(ProviderEventListener listener);

    void publish(ProviderEvent event);
}
