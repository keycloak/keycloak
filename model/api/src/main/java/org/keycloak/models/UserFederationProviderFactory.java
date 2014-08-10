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
     * Sync all users from the provider storage to Keycloak storage.
     *
     * @param sessionFactory
     * @param realmId
     * @param model
     */
    void syncAllUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model);

    /**
     * Sync just changed (added / updated / removed) users from the provider storage to Keycloak storage. This is useful in case
     * that your storage supports "changelogs" (Tracking what users changed since specified date). It's implementation specific to
     * decide what exactly will be changed (For example LDAP supports tracking of added / updated users, but not removed users. So
     * removed users are not synced)
     *
     * @param sessionFactory
     * @param realmId
     * @param model
     * @param lastSync
     */
    void syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync);
}
