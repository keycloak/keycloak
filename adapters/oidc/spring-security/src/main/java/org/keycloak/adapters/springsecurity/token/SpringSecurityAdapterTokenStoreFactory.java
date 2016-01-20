package org.keycloak.adapters.springsecurity.token;

import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link AdapterTokenStoreFactory} that returns a new {@link SpringSecurityTokenStore} for each request.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 */
public class SpringSecurityAdapterTokenStoreFactory implements AdapterTokenStoreFactory {

    @Override
    public AdapterTokenStore createAdapterTokenStore(KeycloakDeployment deployment, HttpServletRequest request) {
        return new SpringSecurityTokenStore(deployment, request);
    }
}
