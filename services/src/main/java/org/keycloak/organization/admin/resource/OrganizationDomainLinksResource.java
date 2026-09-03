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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationDomainLinksResource {

    private final KeycloakSession session;
    private final OrganizationProvider organizationProvider;
    private final OrganizationModel organization;
    private final AdminPermissionEvaluator auth;

    public OrganizationDomainLinksResource(KeycloakSession session, OrganizationModel organization, AdminPermissionEvaluator auth) {
        this.session = session;
        this.organizationProvider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.auth = auth;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Claims an existing domain for the organization",
        description = "Associates an existing domain with the organization. The domain must already exist in the realm. " +
                "If the domain is already claimed by this organization, a CONFLICT error is returned.")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found — domain does not exist"),
        @APIResponse(responseCode = "409", description = "Conflict — domain already claimed by this organization")
    })
    public Response claimDomain(String domainName) {
        auth.orgs().requireManage(organization);
        domainName = domainName.trim().replaceAll("^\"|\"$", "");

        try {
            if (organizationProvider.addDomainToOrganization(organization, domainName)) {
                return Response.noContent().build();
            }
            throw ErrorResponse.error("Domain already claimed by this organization", Status.CONFLICT);
        } catch (ModelValidationException mve) {
            throw ErrorResponse.error(mve.getMessage(), Status.BAD_REQUEST);
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Status.NOT_FOUND);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns all domains claimed by this organization")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OrganizationDomainRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<OrganizationDomainRepresentation> getDomains() {
        auth.orgs().requireView(organization);
        return organization.getDomains().map(this::toRepresentation);
    }

    @Path("{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns a domain claimed by this organization")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OrganizationDomainRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public OrganizationDomainRepresentation getDomain(@PathParam("name") String name) {
        auth.orgs().requireView(organization);
        OrganizationDomainModel domain = organization.getDomains()
                .filter(d -> d.getName().equals(name.trim().toLowerCase()))
                .findFirst()
                .orElse(null);
        if (domain == null) {
            throw ErrorResponse.error("Domain not claimed by this organization", Status.NOT_FOUND);
        }
        return toRepresentation(domain);
    }

    @Path("{name}")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Unclaims a domain from the organization",
        description = "Removes the association between the domain and the organization. The domain itself is not deleted.")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found — domain not claimed by this organization")
    })
    public Response unclaimDomain(@PathParam("name") String name) {
        auth.orgs().requireManage(organization);

        if (!organizationProvider.removeDomainFromOrganization(organization, name)) {
            throw ErrorResponse.error("Domain not claimed by this organization", Status.NOT_FOUND);
        }
        return Response.noContent().build();
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
