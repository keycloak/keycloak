package org.keycloak.scim.services;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import org.jboss.logging.Logger;

public class ScimRealmResource {

    private static final Logger logger = Logger.getLogger(ScimRealmResource.class);

    private final KeycloakSession session;

    public ScimRealmResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("/v2/{resourceType}")
    public Object resourceType(@PathParam("resourceType") String resourceType) {
        ScimResourceTypeProvider<?> provider = session.getProvider(ScimResourceTypeProvider.class, resourceType);

        if (provider == null) {
            logger.debugf("SCIM resource type '%s' not found", resourceType);
            throw new ErrorResponseException(Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse("Resource type not found", Status.NOT_FOUND.getStatusCode()))
                    .build());
        }

        AdminEventBuilder adminEvent = createAdminEventBuilder();

        return new ScimResourceTypeResource<>(session, provider, adminEvent);
    }

    private AdminEventBuilder createAdminEventBuilder() {
        RealmModel realm = session.getContext().getRealm();
        AccessToken token = (AccessToken) session.getContext().getBearerToken();
        AdminAuth auth = new AdminAuth(realm, token, session.getContext().getUser(), session.getContext().getClient());
        return new AdminEventBuilder(realm, auth, session, session.getContext().getConnection());
    }
}
