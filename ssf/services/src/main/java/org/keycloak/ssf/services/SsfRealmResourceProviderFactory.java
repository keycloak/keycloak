package org.keycloak.ssf.services;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.keycloak.ssf.Ssf;

public class SsfRealmResourceProviderFactory implements RealmResourceProviderFactory, EnvironmentDependentProviderFactory {

    /**
     * The SSF endpoints are available under {@code $KC_ISSUER_URL/ssf}.
     */
    @Override
    public String getId() {
        return "ssf";
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {

        if (!Ssf.isTransmitterEnabled(session.getContext().getRealm())) {
            return null;
        }

        return new SsfRealmResourceProvider(session);
    }

    @Override
    public void init(Config.Scope scope) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        keycloakSessionFactory.register(event -> {
            if (event instanceof RealmModel.RealmPostCreateEvent realmPostCreateEvent) {
                SsfBootstrap.addSsfSupport(realmPostCreateEvent.getCreatedRealm());
            } else if (event instanceof RealmModel.RealmAttributeUpdateEvent attrUpdateEvent
                    && Ssf.SSF_TRANSMITTER_ENABLED_KEY.equals(attrUpdateEvent.getAttributeName())
                    && Boolean.parseBoolean(attrUpdateEvent.getAttributeValue())) {
                SsfBootstrap.addSsfSupport(attrUpdateEvent.getRealm());
            }
        });
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }
}
