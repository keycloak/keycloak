package org.keycloak.mappers;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserFederationMapper extends Provider {

    /**
     * Sync data from federation storage to Keycloak. It's useful just if mapper needs some data preloaded from federation storage (For example
     * load roles from federation provider and sync them to Keycloak database)
     *
     * Applicable just if sync is supported (see UserFederationMapperFactory.getSyncConfig() )
     *
     * @see UserFederationMapperFactory#getSyncConfig()
     * @param mapperModel
     * @param federationProvider
     * @param session
     * @param realm
     */
    UserFederationSyncResult syncDataFromFederationProviderToKeycloak(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm);

    /**
     * Sync data from Keycloak back to federation storage
     *
     * @see UserFederationMapperFactory#getSyncConfig()
     * @param mapperModel
     * @param federationProvider
     * @param session
     * @param realm
     */
    UserFederationSyncResult syncDataFromKeycloakToFederationProvider(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm);
}
