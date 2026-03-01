package org.keycloak.scim.services;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.Config.Scope;
import org.keycloak.Token;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ScimRealmResourceFactory implements RealmResourceProviderFactory, EnvironmentDependentProviderFactory {

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();

        if (realm.isScimEnabled()) {
            return new RealmResourceProvider() {

                @Override
                public Object getResource() {
                    Token bearerToken = session.getContext().getBearerToken();

                    if (bearerToken == null) {
                        return Response.status(Status.UNAUTHORIZED).build();
                    }

                    return new ScimRealmResource(session);
                }

                @Override
                public void close() {

                }
            };
        }

        return null;
    }

    @Override
    public void init(Scope config) {
        config.toString();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.toString();
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "scim";
    }

    @Override
    public boolean isSupported(Scope config) {
        return Profile.isFeatureEnabled(Feature.SCIM_API);
    }
}
