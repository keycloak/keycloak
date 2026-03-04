package org.keycloak.scim.services;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;

public class ScimRealmResource {

    private final KeycloakSession session;

    public ScimRealmResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("/v2/{resourceType}")
    public Object resourceType(@PathParam("resourceType") String resourceType) {
        ScimResourceTypeProvider<?> provider = session.getProvider(ScimResourceTypeProvider.class, resourceType);// Ensure the provider is loaded

        if (provider == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse("Resource type not found", Status.NOT_FOUND.getStatusCode())).build();
        }

        return new ScimResourceTypeResource<>(session, provider);
    }

    @Path("/organizations/{organization}/v2/{resourceType}")
    public Object resourceType(@PathParam("organization") String alias, @PathParam("resourceType") String resourceType) {
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);

        if (!orgProvider.isEnabled()) {
            return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse("Organization not found", Status.NOT_FOUND.getStatusCode())).build();
        }

        OrganizationModel organization = orgProvider.getByAlias(alias);

        if (organization == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse("Organization not found", Status.NOT_FOUND.getStatusCode())).build();
        }

        session.getContext().setOrganization(organization);

        ScimResourceTypeProvider<?> provider = session.getProvider(ScimResourceTypeProvider.class, resourceType);// Ensure the provider is loaded

        if (provider == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse("Resource type not found", Status.NOT_FOUND.getStatusCode())).build();
        }

        return new ScimResourceTypeResource<>(session, provider);
    }
}
