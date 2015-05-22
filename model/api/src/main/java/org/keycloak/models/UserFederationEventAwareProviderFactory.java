package org.keycloak.models;

import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

/**
 * Provides "onProviderModelCreated" callback  invoked when UserFederationProviderModel for this factory implementation is created in realm
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class UserFederationEventAwareProviderFactory implements UserFederationProviderFactory {

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof RealmModel.UserFederationProviderCreationEvent) {
                    RealmModel.UserFederationProviderCreationEvent fedCreationEvent = (RealmModel.UserFederationProviderCreationEvent)event;
                    UserFederationProviderModel providerModel = fedCreationEvent.getCreatedFederationProvider();

                    if (providerModel.getProviderName().equals(getId())) {
                        onProviderModelCreated(fedCreationEvent.getRealm(), providerModel);
                    }
                }
            }

        });
    }

    protected abstract void onProviderModelCreated(RealmModel realm, UserFederationProviderModel createdProviderModel);
}
