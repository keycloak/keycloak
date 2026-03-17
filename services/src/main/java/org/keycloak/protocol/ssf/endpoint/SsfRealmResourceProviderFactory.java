package org.keycloak.protocol.ssf.endpoint;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.transmitter.SsfScopes;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class SsfRealmResourceProviderFactory implements RealmResourceProviderFactory, EnvironmentDependentProviderFactory {

    // Shared instance is safe here because SsfRealmResourceProvider is stateless.
    // Do not add mutable fields to SsfRealmResourceProvider without changing this to per-request creation.
    private static final SsfRealmResourceProvider INSTANCE = new SsfRealmResourceProvider();

    /**
     * The SSF endpoints are available under {@code $KC_ISSUER_URL/ssf}.
     *
     * @return
     */
    @Override
    public String getId() {
        return "ssf";
    }

    @Override
    public RealmResourceProvider create(KeycloakSession keycloakSession) {
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope scope) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        keycloakSessionFactory.register(event -> {
            if (event instanceof RealmModel.RealmPostCreateEvent realmPostCreateEvent) {
                SsfScopes.createDefaultClientScopes(realmPostCreateEvent.getCreatedRealm());
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
