package org.keycloak.adapters.springsecurity.token;

import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;

import javax.servlet.http.HttpServletRequest;

/**
 * Creates a per-request adapter token store.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 */
public interface AdapterTokenStoreFactory {

    /**
     * Returns a new {@link AdapterTokenStore} for the given {@link KeycloakDeployment} and {@link HttpServletRequest request}.
     *
     * @param deployment the <code>KeycloakDeployment</code> (required)
     * @param request the current <code>HttpServletRequest</code> (required)
     *
     * @return a new <code>AdapterTokenStore</code> for the given <code>deployment</code> and <code>request</code>
     * @throws IllegalArgumentException if either the <code>deployment</code> or <code>request</code> is <code>null</code>
     */
    AdapterTokenStore createAdapterTokenStore(KeycloakDeployment deployment, HttpServletRequest request);

}
