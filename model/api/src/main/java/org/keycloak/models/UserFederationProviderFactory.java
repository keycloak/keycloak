package org.keycloak.models;

import org.keycloak.provider.ProviderFactory;

import java.util.Date;
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

    /**
     * Sync all users from the provider storage to Keycloak storage. Alternatively can update existing users or remove keycloak users, which are no longer
     * available in federation storage (depends on the implementation)
     *
     * @param sessionFactory
     * @param realmId
     * @param model
     * @return result with count of added/updated/removed users
     */
    UserFederationSyncResult syncAllUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model);

    /**
     * Sync just changed (added / updated / removed) users from the provider storage to Keycloak storage. This is useful in case
     * that your storage supports "changelogs" (Tracking what users changed since specified date). It's implementation specific to
     * decide what exactly will be changed
     *
     * @param sessionFactory
     * @param realmId
     * @param model
     * @param lastSync
     */
    UserFederationSyncResult syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync);

    /**
     * This method is never called and is only an artifact of ProviderFactory.  Returning null with no implementation is recommended.
     * @param session
     * @return
     */
    @Override
    UserFederationProvider create(KeycloakSession session);
}
