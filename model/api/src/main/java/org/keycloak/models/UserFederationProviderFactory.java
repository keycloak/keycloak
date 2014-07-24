package org.keycloak.models;

import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserFederationProviderFactory extends ProviderFactory<UserFederationProvider> {
    UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model);
}
