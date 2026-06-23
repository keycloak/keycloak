package org.keycloak.scim.services;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.Config.Scope;
import org.keycloak.Token;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import org.jboss.logging.Logger;

public class ScimRealmResourceFactory implements RealmResourceProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(ScimRealmResourceFactory.class);

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();

        if (realm.isScimApiEnabled()) {
            return new RealmResourceProvider() {

                @Override
                public Object getResource() {
                    AuthResult authResult = new BearerTokenAuthenticator(session).authenticate();

                    if (authResult == null) {
                        logger.debug("SCIM request rejected: no valid bearer token provided");
                        throw new ErrorResponseException(Response.status(Status.UNAUTHORIZED)
                                .type(MediaType.APPLICATION_JSON)
                                .entity(new ErrorResponse("Bearer token required", Status.UNAUTHORIZED.getStatusCode()))
                                .build());
                    }

                    Token bearerToken = session.getContext().getBearerToken();

                    if (bearerToken == null) {
                        logger.debug("SCIM request rejected: bearer token could not be resolved");
                        throw new ErrorResponseException(Response.status(Status.UNAUTHORIZED)
                                .type(MediaType.APPLICATION_JSON)
                                .entity(new ErrorResponse("Bearer token required", Status.UNAUTHORIZED.getStatusCode()))
                                .build());
                    }

                    ClientModel client = authResult.client();

                    if (client.isPublicClient()) {
                        logger.debug("SCIM request rejected: public clients not allowed");
                        throw new ErrorResponseException(Response.status(Status.FORBIDDEN)
                                .type(MediaType.APPLICATION_JSON)
                                .entity(new ErrorResponse("Public client not allowed", Status.FORBIDDEN.getStatusCode()))
                                .build());
                    }

                    return new ScimRealmResource(session);
                }

                @Override
                public void close() {

                }
            };
        }

        logger.warnf("SCIM API is not enabled for realm '%s'", realm.getName());
        return null;
    }

    @Override
    public void init(Scope config) {
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
