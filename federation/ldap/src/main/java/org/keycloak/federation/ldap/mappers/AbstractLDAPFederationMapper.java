package org.keycloak.federation.ldap.mappers;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationSyncResult;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPFederationMapper implements LDAPFederationMapper {

    @Override
    public UserFederationSyncResult syncDataFromFederationProviderToKeycloak(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public UserFederationSyncResult syncDataFromKeycloakToFederationProvider(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public void close() {

    }

    protected boolean parseBooleanParameter(UserFederationMapperModel mapperModel, String paramName) {
        String paramm = mapperModel.getConfig().get(paramName);
        return Boolean.parseBoolean(paramm);
    }
}
