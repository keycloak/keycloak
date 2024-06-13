/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.organization.admin.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;

@Provider
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationIdentityProvidersResource {

    private final RealmModel realm;
    private final OrganizationProvider organizationProvider;
    private final OrganizationModel organization;

    public OrganizationIdentityProvidersResource() {
        // needed for registering to the JAX-RS stack
        this(null, null, null);
    }

    public OrganizationIdentityProvidersResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent) {
        this.realm = session == null ? null : session.getContext().getRealm();
        this.organizationProvider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.organization = organization;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Adds the identity provider with the specified id to the organization",
        description = "Adds, or associates, an existing identity provider with the organization. If no identity provider is found, " +
                "or if it is already associated with the organization, an error response is returned")
    public Response addIdentityProvider(String id) {
        try {
            IdentityProviderModel identityProvider =  this.realm.getIdentityProvidersStream()
                    .filter(p -> Objects.equals(p.getAlias(), id) || Objects.equals(p.getInternalId(), id))
                    .findFirst().orElse(null);

            if (identityProvider == null) {
                throw ErrorResponse.error("Identity provider not found with the given alias", Status.BAD_REQUEST);
            }

            if (organizationProvider.addIdentityProvider(organization, identityProvider)) {
                return Response.noContent().build();
            }

            throw ErrorResponse.error("Identity provider already associated to the organization", Status.CONFLICT);
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Status.BAD_REQUEST);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns all identity providers associated with the organization")
    public Stream<IdentityProviderRepresentation> getIdentityProviders() {
        return organization.getIdentityProviders().map(this::toRepresentation);
    }

    @Path("{alias}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns the identity provider associated with the organization that has the specified alias",
        description = "Searches for an identity provider with the given alias. If one is found and is associated with the " +
                "organization, it is returned. Otherwise, an error response with status NOT_FOUND is returned")
    public IdentityProviderRepresentation getIdentityProvider(@PathParam("alias") String alias) {
        IdentityProviderModel broker = realm.getIdentityProviderByAlias(alias);

        if (!isOrganizationBroker(broker)) {
            throw ErrorResponse.error("Identity provider not associated with the organization", Status.NOT_FOUND);
        }

        return ModelToRepresentation.toRepresentation(realm, broker);
    }

    @Path("{alias}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Removes the identity provider with the specified alias from the organization",
        description = "Breaks the association between the identity provider and the organization. The provider itself is not deleted. " +
                "If no provider is found, or if it is not currently associated with the org, an error response is returned")
    public Response delete(@PathParam("alias") String alias) {
        IdentityProviderModel broker = realm.getIdentityProviderByAlias(alias);

        if (!isOrganizationBroker(broker)) {
            throw ErrorResponse.error("Identity provider not found with the given alias", Status.NOT_FOUND);
        }

        if (organizationProvider.removeIdentityProvider(organization, broker)) {
            return Response.noContent().build();
        }

        throw ErrorResponse.error("Identity provider not associated with the organization", Status.BAD_REQUEST);
    }

    private IdentityProviderRepresentation toRepresentation(IdentityProviderModel idp) {
        return ModelToRepresentation.toRepresentation(realm, idp);
    }

    private boolean isOrganizationBroker(IdentityProviderModel broker) {
        return broker != null && organization.getId().equals(broker.getOrganizationId());
    }
}
