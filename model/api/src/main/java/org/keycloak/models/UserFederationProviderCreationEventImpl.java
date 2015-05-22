package org.keycloak.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationProviderCreationEventImpl implements RealmModel.UserFederationProviderCreationEvent {

    private final UserFederationProviderModel createdFederationProvider;
    private final RealmModel realm;

    public UserFederationProviderCreationEventImpl(RealmModel realm, UserFederationProviderModel createdFederationProvider) {
        this.realm = realm;
        this.createdFederationProvider = createdFederationProvider;
    }

    @Override
    public UserFederationProviderModel getCreatedFederationProvider() {
        return createdFederationProvider;
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }
}
