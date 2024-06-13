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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;

@Provider
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationResource {

    private final KeycloakSession session;
    private final OrganizationProvider provider;
    private final AdminEventBuilder adminEvent;
    private final OrganizationModel organization;

    public OrganizationResource() {
        // needed for registering to the JAX-RS stack
        this(null, null, null);
    }

    public OrganizationResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent) {
        this.session = session;
        this.provider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.adminEvent = adminEvent;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns the organization representation")
    public OrganizationRepresentation get() {
        return Organizations.toRepresentation(organization);
    }

    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Deletes the organization")
    public Response delete() {
        provider.remove(organization);
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Updates the organization")
    public Response update(OrganizationRepresentation organizationRep) {
        try {
            Organizations.toModel(organizationRep, organization);
            return Response.noContent().build();
        } catch (ModelValidationException mve) {
            throw ErrorResponse.error(mve.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @Path("members")
    public OrganizationMemberResource members() {
        return new OrganizationMemberResource(session, organization, adminEvent);
    }

    @Path("identity-providers")
    public OrganizationIdentityProvidersResource identityProvider() {
        return new OrganizationIdentityProvidersResource(session, organization, adminEvent);
    }
}
