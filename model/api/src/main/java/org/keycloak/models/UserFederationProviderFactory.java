package org.keycloak.models;

import org.keycloak.provider.ProviderFactory;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserFederationProviderFactory extends ProviderFactory<UserFederationProvider> {
    /**
     * called per Keycloak transaction.
     *
     * @param session
     * @param model
     * @return
     */
    UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model);

    /**
     * Config options to display in generic admin console page for federation
     *
     * @return
     */
    Set<String> getConfigurationOptions();

    /**
     * This is the name of the provider and will be showed in the admin console as an option.
     *
     * @return
     */
    @Override
    String getId();
}
