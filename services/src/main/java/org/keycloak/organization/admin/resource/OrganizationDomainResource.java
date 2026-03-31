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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationDomainResource {

    private final KeycloakSession session;
    private final OrganizationModel organization;
    private final AdminEventBuilder adminEvent;

    public OrganizationDomainResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent) {
        this.session = session;
        this.organization = organization;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns the organization domains")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = OrganizationDomainRepresentation.class, type = SchemaType.ARRAY)))
    })
    public Stream<OrganizationDomainRepresentation> getDomains() {
        return organization.getDomains().map(ModelToRepresentation::toRepresentation);
    }

    @Path("{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns a specific domain of the organization")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = OrganizationDomainRepresentation.class))),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Response getDomain(
            @Parameter(description = "The domain name") @PathParam("name") String name) {
        OrganizationDomainRepresentation domain = organization.getDomains()
                .filter(d -> d.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(ModelToRepresentation::toRepresentation)
                .orElse(null);
        
        if (domain == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        return Response.ok(domain).build();
    }

    @Path("{name}")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Partially updates a domain of the organization",
            description = "Updates a domain using a subset of the representation. Only the provided fields are updated.")
    @RequestBody(description = "Subset of the domain representation", required = true)
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Updated", content = @Content(schema = @Schema(implementation = OrganizationDomainRepresentation.class))),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Response updateDomain(
            @Parameter(description = "The domain name") @PathParam("name") String name,
            OrganizationDomainRepresentation representation) {
        OrganizationDomainModel existingDomain = organization.getDomains()
                .filter(d -> d.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        
        if (existingDomain == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        // We only allow to update the idp id
        String idpId = representation.getIdpId() != null 
                ? representation.getIdpId() 
                : existingDomain.getIdpId();
        
        OrganizationDomainModel updatedDomain = new OrganizationDomainModel(
                existingDomain.getName(),
                existingDomain.isVerified(),
                idpId
        );
        
        // Update the domain in the organization's domains set
        java.util.Set<OrganizationDomainModel> domains = organization.getDomains().collect(java.util.stream.Collectors.toSet());
        domains.remove(existingDomain);
        domains.add(updatedDomain);
        organization.setDomains(domains);

        OrganizationDomainRepresentation updatedRep = ModelToRepresentation.toRepresentation(updatedDomain);
        adminEvent.operation(OperationType.UPDATE)
                .resourcePath(session.getContext().getUri())
                .representation(updatedRep).success();

        return Response.ok(updatedRep).build();
    }
}
