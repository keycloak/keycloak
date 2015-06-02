package org.keycloak.provider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ProviderEventListener {
    void onEvent(ProviderEvent event);
}
