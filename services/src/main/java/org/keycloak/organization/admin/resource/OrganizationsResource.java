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


import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.organization.validation.OrganizationsValidation;
import org.keycloak.organization.validation.OrganizationsValidation.OrganizationValidationException;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.SearchQueryUtils;
import org.keycloak.utils.StringUtil;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationsResource {

    private final KeycloakSession session;
    private final OrganizationProvider provider;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    private static final Logger logger = Logger.getLogger(OrganizationsResource.class);

    public OrganizationsResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.provider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION);
    }

    /**
     * Creates a new organization based on the specified {@link OrganizationRepresentation}.
     *
     * @param organization the representation containing the organization data.
     * @return a {@link Response} containing the status of the operation.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation( summary = "Creates a new organization")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response create(OrganizationRepresentation organization) {
        auth.realm().requireManageRealm();
        Organizations.checkEnabled(provider);

        if (organization == null) {
            throw ErrorResponse.error("Organization cannot be null.", Response.Status.BAD_REQUEST);
        }

        ReservedCharValidator.validateNoSpace(organization.getAlias());

        try {
            OrganizationsValidation.validateUrl(organization.getRedirectUrl());

            OrganizationModel model = provider.create(organization.getName(), organization.getAlias());
            RepresentationToModel.toModel(organization, model);
            organization.setId(model.getId());
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), model.getId()).representation(organization).success();
            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
        } catch (ModelValidationException | OrganizationValidationException ex) {
            throw ErrorResponse.error(ex.getMessage(), Response.Status.BAD_REQUEST);
        } catch (ModelDuplicateException mde) {
            throw ErrorResponse.error(mde.getMessage(), Status.CONFLICT);
        }
    }

    /**
     * Returns a stream of organizations, filtered according to query parameters.
     *
     * @param search a {@code String} representing either an organization name or domain.
     * @param searchQuery a query to search for organization attributes, in the format 'key1:value2 key2:value2'.
     * @param exact if {@code true}, the organizations will be searched using exact match for the {@code search} param - i.e.
     *              either the organization name or one of its domains must match exactly the {@code search} param. If false,
     *              the method returns all organizations whose name or (domains) partially match the {@code search} param.
     * @param first the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return a non-null {@code Stream} of matched organizations.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns a paginated list of organizations filtered according to the specified parameters")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = OrganizationRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Stream<OrganizationRepresentation> search(
            @Parameter(description = "A String representing either an organization name or domain") @QueryParam("search") String search,
            @Parameter(description = "A query to search for custom attributes, in the format 'key1:value2 key2:value2'") @QueryParam("q") String searchQuery,
            @Parameter(description = "Boolean which defines whether the param 'search' must match exactly or not") @QueryParam("exact") Boolean exact,
            @Parameter(description = "The position of the first result to be processed (pagination offset)") @QueryParam("first") @DefaultValue("0") Integer first,
            @Parameter(description = "The maximum number of results to be returned - defaults to 10") @QueryParam("max") @DefaultValue("10") Integer max,
            @Parameter(description = "if false, return the full representation. Otherwise, only the basic fields are returned.") @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation
    ) {
        auth.realm().requireManageRealm();
        Organizations.checkEnabled(provider);

        // check if are searching orgs by attribute.
        if (StringUtil.isNotBlank(searchQuery)) {
            Map<String, String> attributes = SearchQueryUtils.getFields(searchQuery);
            return provider.getAllStream(attributes, first, max).map(model -> ModelToRepresentation.toRepresentation(model, briefRepresentation));
        } else {
            return provider.getAllStream(search, exact, first, max).map(model -> ModelToRepresentation.toRepresentation(model, briefRepresentation));
        }
    }

    /**
     * Base path for the admin REST API for one particular organization.
     */
    @Path("{org-id}")
    public OrganizationResource get(@PathParam("org-id") String orgId) {
        auth.realm().requireManageRealm();
        Organizations.checkEnabled(provider);

        if (StringUtil.isBlank(orgId)) {
            throw ErrorResponse.error("Id cannot be null.", Response.Status.BAD_REQUEST);
        }

        OrganizationModel organizationModel = provider.getById(orgId);

        if (organizationModel == null) {
            throw ErrorResponse.error("Organization not found.", Response.Status.NOT_FOUND);
        }

        session.getContext().setOrganization(organizationModel);

        return new OrganizationResource(session, organizationModel, adminEvent);
    }

    /**
     * Returns the organizations counts.
     *
     * @return
     */
    @GET
    @NoCache
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns the organizations counts.")
    public long getOrganizationCount(
            @Parameter(description = "A String representing either an organization name or domain") @QueryParam("search") String search,
            @Parameter(description = "A query to search for custom attributes, in the format 'key1:value2 key2:value2'") @QueryParam("q") String searchQuery,
            @Parameter(description = "Boolean which defines whether the param 'search' must match exactly or not") @QueryParam("exact") Boolean exact
    ) {
        auth.realm().requireManageRealm();
        Organizations.checkEnabled(provider);

        if (StringUtil.isNotBlank(searchQuery)) {
            Map<String, String> attributes = SearchQueryUtils.getFields(searchQuery);
            return provider.count(attributes);
        }
        return provider.count(search, exact);
    }

    @Path("members/{member-id}/organizations")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns the organizations associated with the user that has the specified id")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = OrganizationRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public Stream<OrganizationRepresentation> getOrganizations(
            @PathParam("member-id") String memberId,
            @Parameter(description = "if false, return the full representation. Otherwise, only the basic fields are returned.")
            @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        return new OrganizationMemberResource(session, null, adminEvent).getOrganizations(memberId, briefRepresentation);
    }
}
