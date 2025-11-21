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

import java.util.stream.Stream;

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

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationIdentityProvidersResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final OrganizationProvider organizationProvider;
    private final OrganizationModel organization;

    public OrganizationIdentityProvidersResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent) {
        this.realm = session == null ? null : session.getContext().getRealm();
        this.session = session;
        this.organizationProvider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.organization = organization;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Adds the identity provider with the specified id to the organization",
        description = "Adds, or associates, an existing identity provider with the organization. If no identity provider is found, " +
                "or if it is already associated with the organization, an error response is returned")
    @RequestBody(description = "Payload should contain only id or alias of the identity provider to be associated with the organization " + 
                "(id or alias with or without quotes). Surrounding whitespace characters will be trimmed.", required = true)
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response addIdentityProvider(String id) {
        id = id.trim().replaceAll("^\"|\"$", ""); // fixes https://github.com/keycloak/keycloak/issues/34401
        
        try {
            IdentityProviderModel identityProvider = session.identityProviders().getByIdOrAlias(id);

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
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = IdentityProviderRepresentation.class, type = SchemaType.ARRAY)))
    })
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
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = IdentityProviderRepresentation.class))),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public IdentityProviderRepresentation getIdentityProvider(@PathParam("alias") String alias) {
        IdentityProviderModel broker = session.identityProviders().getByAlias(alias);

        if (!isOrganizationBroker(broker)) {
            throw ErrorResponse.error("Identity provider not associated with the organization", Status.NOT_FOUND);
        }

        return toRepresentation(broker);
    }

    @Path("{alias}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Removes the identity provider with the specified alias from the organization",
        description = "Breaks the association between the identity provider and the organization. The provider itself is not deleted. " +
                "If no provider is found, or if it is not currently associated with the org, an error response is returned")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Response delete(@PathParam("alias") String alias) {
        IdentityProviderModel broker = session.identityProviders().getByAlias(alias);

        if (!isOrganizationBroker(broker)) {
            throw ErrorResponse.error("Identity provider not found with the given alias", Status.NOT_FOUND);
        }

        if (organizationProvider.removeIdentityProvider(organization, broker)) {
            return Response.noContent().build();
        }

        throw ErrorResponse.error("Identity provider not associated with the organization", Status.BAD_REQUEST);
    }

    private IdentityProviderRepresentation toRepresentation(IdentityProviderModel idp) {
        return StripSecretsUtils.stripSecrets(session, ModelToRepresentation.toRepresentation(session, realm, idp));
    }

    private boolean isOrganizationBroker(IdentityProviderModel broker) {
        return broker != null && organization.getId().equals(broker.getOrganizationId());
    }
}
