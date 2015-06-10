package org.keycloak.models;

/**
 * Called during creation or update of UserFederationMapperModel
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationMapperEventImpl implements RealmModel.UserFederationMapperEvent {

    private final UserFederationMapperModel mapperModel;
    private final RealmModel realm;
    private final KeycloakSession session;

    public UserFederationMapperEventImpl(UserFederationMapperModel mapperModel, RealmModel realm, KeycloakSession session) {
        this.mapperModel = mapperModel;
        this.realm = realm;
        this.session = session;
    }

    @Override
    public UserFederationMapperModel getFederationMapper() {
        return mapperModel;
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    public KeycloakSession getSession() {
        return session;
    }
}
