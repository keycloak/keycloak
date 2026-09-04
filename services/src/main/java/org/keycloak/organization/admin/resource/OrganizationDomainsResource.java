/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationDomainsResource {

    private final KeycloakSession session;
    private final OrganizationProvider organizationProvider;
    private final AdminPermissionEvaluator auth;

    public OrganizationDomainsResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.organizationProvider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.auth = auth;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Creates a new domain in the realm",
        description = "Creates a domain that can later be claimed by organizations. The domain is not associated with any organization upon creation.")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response create(OrganizationDomainRepresentation rep) {
        auth.orgs().requireManage();

        try {
            OrganizationDomainModel domain = organizationProvider.createDomain(
                    rep.getName(), rep.isVerified(),
                    rep.getIdentityProviderAlias(), rep.isAutoRedirect());

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder()
                    .path(domain.getName()).build()).build();
        } catch (ModelValidationException mve) {
            throw ErrorResponse.error(mve.getMessage(), Status.BAD_REQUEST);
        } catch (ModelDuplicateException mde) {
            throw ErrorResponse.error(mde.getMessage(), Status.CONFLICT);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns all domains in the realm")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OrganizationDomainRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<OrganizationDomainRepresentation> getAll(
            @Parameter(description = "Search by domain name") @QueryParam("search") String search,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer first,
            @Parameter(description = "Maximum results") @QueryParam("max") Integer max) {
        auth.orgs().requireQuery();
        return organizationProvider.getDomains(search, first, max).map(this::toRepresentation);
    }

    @Path("{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns a domain by its name")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OrganizationDomainRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public OrganizationDomainRepresentation get(@PathParam("name") String name) {
        auth.orgs().requireQuery();
        OrganizationDomainModel domain = organizationProvider.getDomainByName(name);
        if (domain == null) {
            throw ErrorResponse.error("Domain not found", Status.NOT_FOUND);
        }
        return toRepresentation(domain);
    }

    @Path("{name}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Updates a domain's properties",
        description = "Updates the identity provider routing and other properties of the domain")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Response update(@PathParam("name") String name, OrganizationDomainRepresentation rep) {
        auth.orgs().requireManage();

        try {
            organizationProvider.updateDomain(name, rep.isVerified(),
                    rep.getIdentityProviderAlias(), rep.isAutoRedirect());
            return Response.noContent().build();
        } catch (ModelValidationException mve) {
            throw ErrorResponse.error(mve.getMessage(), Status.BAD_REQUEST);
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Status.NOT_FOUND);
        }
    }

    @Path("{name}")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Deletes a domain",
        description = "Deletes a domain only if it is not claimed by any organization")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found"),
        @APIResponse(responseCode = "409", description = "Conflict — domain is still claimed by an organization")
    })
    public Response delete(@PathParam("name") String name) {
        auth.orgs().requireManage();

        try {
            if (!organizationProvider.removeDomain(name)) {
                throw ErrorResponse.error("Domain not found", Status.NOT_FOUND);
            }
            return Response.noContent().build();
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Status.CONFLICT);
        }
    }

    private OrganizationDomainRepresentation toRepresentation(OrganizationDomainModel model) {
        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName(model.getName());
        rep.setVerified(model.isVerified());
        rep.setIdentityProviderAlias(model.getIdentityProviderAlias());
        rep.setAutoRedirect(model.isAutoRedirect());
        return rep;
    }
}
