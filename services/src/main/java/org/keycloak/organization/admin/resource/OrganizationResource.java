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

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.utils.SearchQueryUtils;
import org.keycloak.utils.StringUtil;

@Provider
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationResource {

    private final KeycloakSession session;
    private final OrganizationProvider provider;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public OrganizationResource() {
        // needed for registering to the JAX-RS stack
        this(null, null, null);
    }

    public OrganizationResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.provider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.auth = auth;
        this.adminEvent = adminEvent;
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
    public Response create(OrganizationRepresentation organization) {
        auth.realm().requireManageRealm();
        checkOrganizationsEnabled();
        if (organization == null) {
            throw ErrorResponse.error("Organization cannot be null.", Response.Status.BAD_REQUEST);
        }

        Set<String> domains = ofNullable(organization.getDomains()).orElse(Set.of()).stream().map(OrganizationDomainRepresentation::getName).collect(Collectors.toSet());
        OrganizationModel model = provider.create(organization.getName(), domains);

        toModel(organization, model);

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
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
    @Operation( summary = "Returns a paginated list of organizations filtered according to the specified parameters")
    public Stream<OrganizationRepresentation> search(
            @Parameter(description = "A String representing either an organization name or domain") @QueryParam("search") String search,
            @Parameter(description = "A query to search for custom attributes, in the format 'key1:value2 key2:value2'") @QueryParam("q") String searchQuery,
            @Parameter(description = "Boolean which defines whether the param 'search' must match exactly or not") @QueryParam("exact") Boolean exact,
            @Parameter(description = "The position of the first result to be processed (pagination offset)") @QueryParam("first") @DefaultValue("0") Integer first,
            @Parameter(description = "The maximum number of results to be returned - defaults to 10") @QueryParam("max") @DefaultValue("10") Integer max
            ) {
        auth.realm().requireManageRealm();
        checkOrganizationsEnabled();

        // check if are searching orgs by attribute.
        if(StringUtil.isNotBlank(searchQuery)) {
            Map<String, String> attributes = SearchQueryUtils.getFields(searchQuery);
            return provider.getAllStream(attributes, first, max).map(this::toRepresentation);
        } else {
            return provider.getAllStream(search, exact, first, max).map(this::toRepresentation);
        }
    }

    /**
     * Returns the organization associated with the specified {@code id}.
     *
     * @param id the organization id.
     * @return the organization associated with the specified id, or {@code null} if no organization is found.
     */
    @Path("{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns the organization associated with the specified id, or null if no organization is found")
    public OrganizationRepresentation get(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        checkOrganizationsEnabled();
        if (StringUtil.isBlank(id)) {
            throw ErrorResponse.error("Id cannot be null.", Response.Status.BAD_REQUEST);
        }

        return toRepresentation(getOrganization(id));
    }

    @Path("{id}")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Deletes the organization with the specified id")
    public Response delete(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        checkOrganizationsEnabled();
        if (StringUtil.isBlank(id)) {
            throw ErrorResponse.error("Id cannot be null.", Response.Status.BAD_REQUEST);
        }

        provider.remove(getOrganization(id));

        return Response.noContent().build();
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Updates the organization with the specified id")
    public Response update(@PathParam("id") String id, OrganizationRepresentation organization) {
        auth.realm().requireManageRealm();
        checkOrganizationsEnabled();
        OrganizationModel model = getOrganization(id);
        toModel(organization, model);

        return Response.noContent().build();
    }

    @Path("{id}/members")
    public OrganizationMemberResource members(@PathParam("id") String id) {
        checkOrganizationsEnabled();
        return new OrganizationMemberResource(session, getOrganization(id), auth, adminEvent);
    }

    @Path("{id}/identity-providers")
    public OrganizationIdentityProvidersResource identityProvider(@PathParam("id") String id) {
        checkOrganizationsEnabled();
        return new OrganizationIdentityProvidersResource(session, getOrganization(id), auth, adminEvent);
    }
    
    private OrganizationModel getOrganization(String id) {
        //checking permissions before trying to find organization to be able to return 403 (instead of 400 or 404)
        auth.realm().requireManageRealm();

        if (id == null) {
            throw ErrorResponse.error("Id cannot be null.", Response.Status.BAD_REQUEST);
        }

        OrganizationModel model = provider.getById(id);

        if (model == null) {
            throw ErrorResponse.error("Organization not found.", Response.Status.NOT_FOUND);
        }

        session.setAttribute(OrganizationModel.class.getName(), model);

        return model;
    }

    private OrganizationRepresentation toRepresentation(OrganizationModel model) {
        if (model == null) {
            return null;
        }

        OrganizationRepresentation rep = new OrganizationRepresentation();

        rep.setId(model.getId());
        rep.setName(model.getName());
        rep.setEnabled(model.isEnabled());
        rep.setDescription(model.getDescription());
        rep.setAttributes(model.getAttributes());
        model.getDomains().filter(Objects::nonNull).map(this::toRepresentation)
                .forEach(rep::addDomain);

        return rep;
    }

    private OrganizationDomainRepresentation toRepresentation(OrganizationDomainModel model) {
        OrganizationDomainRepresentation representation = new OrganizationDomainRepresentation();
        representation.setName(model.getName());
        representation.setVerified(model.getVerified());
        return representation;
    }

    private OrganizationModel toModel(OrganizationRepresentation rep, OrganizationModel model) {
        if (rep == null) {
            return null;
        }

        model.setName(rep.getName());
        model.setEnabled(rep.isEnabled());
        model.setDescription(rep.getDescription());
        model.setAttributes(rep.getAttributes());
        model.setDomains(ofNullable(rep.getDomains()).orElse(Set.of()).stream()
                    .filter(Objects::nonNull)
                    .map(this::toModel)
                    .collect(Collectors.toSet()));

        return model;
    }

    private OrganizationDomainModel toModel(OrganizationDomainRepresentation domainRepresentation) {
        return new OrganizationDomainModel(domainRepresentation.getName(), domainRepresentation.isVerified());
    }

    private void checkOrganizationsEnabled() {
        if (provider != null && !provider.isEnabled()) {
            throw ErrorResponse.error("Organizations not enabled for this realm.", Response.Status.NOT_FOUND);
        }
    }
}
