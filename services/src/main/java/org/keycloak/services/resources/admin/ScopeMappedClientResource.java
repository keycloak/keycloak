/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.resources.admin;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

/**
 * @resource Scope Mappings
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ScopeMappedClientResource {
    protected RealmModel realm;
    protected AdminPermissionEvaluator auth;
    protected AdminPermissionEvaluator.RequirePermissionCheck managePermission;
    protected AdminPermissionEvaluator.RequirePermissionCheck viewPermission;
    protected ScopeContainerModel scopeContainer;
    protected KeycloakSession session;
    protected ClientModel scopedClient;
    protected AdminEventBuilder adminEvent;
    
    public ScopeMappedClientResource(RealmModel realm, AdminPermissionEvaluator auth, ScopeContainerModel scopeContainer, KeycloakSession session, ClientModel scopedClient, AdminEventBuilder adminEvent,
                                     AdminPermissionEvaluator.RequirePermissionCheck managePermission,
                                     AdminPermissionEvaluator.RequirePermissionCheck viewPermission) {
        this.realm = realm;
        this.auth = auth;
        this.scopeContainer = scopeContainer;
        this.session = session;
        this.scopedClient = scopedClient;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_SCOPE_MAPPING);
        this.managePermission = managePermission;
        this.viewPermission = viewPermission;
    }

    /**
     * Get the roles associated with a client's scope
     *
     * Returns roles for the client.
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Get the roles associated with a client's scope Returns roles for the client.")
    public Stream<RoleRepresentation> getClientScopeMappings() {
        viewPermission.require();

        return KeycloakModelUtils.getClientScopeMappingsStream(scopedClient, scopeContainer)
                .map(ModelToRepresentation::toBriefRepresentation);
    }

    /**
     * The available client-level roles
     *
     * Returns the roles for the client that can be associated with the client's scope
     *
     * @return
     */
    @Path("available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "The available client-level roles Returns the roles for the client that can be associated with the client's scope")
    public Stream<RoleRepresentation> getAvailableClientScopeMappings() {
        viewPermission.require();

        return scopedClient.getRolesStream()
                .filter(((Predicate<RoleModel>) scopeContainer::hasDirectScope).negate())
                .filter(auth.roles()::canMapClientScope)
                .map(ModelToRepresentation::toBriefRepresentation);
    }

    /**
     * Get effective client roles
     *
     * Returns the roles for the client that are associated with the client's scope.
     *
     * @param briefRepresentation if false, return roles with their attributes
     * 
     * @return
     */
    @Path("composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Get effective client roles Returns the roles for the client that are associated with the client's scope.")
    public Stream<RoleRepresentation> getCompositeClientScopeMappings(@Parameter(description = "if false, return roles with their attributes") @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        viewPermission.require();

        Function<RoleModel, RoleRepresentation> toBriefRepresentation = briefRepresentation ?
                ModelToRepresentation::toBriefRepresentation : ModelToRepresentation::toRepresentation;
        return scopedClient.getRolesStream()
                .filter(scopeContainer::hasScope)
                .map(toBriefRepresentation);
    }

    /**
     * Add client-level roles to the client's scope
     *
     * @param roles
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Add client-level roles to the client's scope")
    @APIResponse(responseCode = "204", description = "No Content")
    public void addClientScopeMapping(List<RoleRepresentation> roles) {
        managePermission.require();

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = scopedClient.getRole(role.getName());
            if (roleModel == null) {
                throw new NotFoundException("Role not found");
            }
            scopeContainer.addScopeMapping(roleModel);
        }

        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(roles).success();
    }

    /**
     * Remove client-level roles from the client's scope.
     *
     * @param roles
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Remove client-level roles from the client's scope.")
    public void deleteClientScopeMapping(List<RoleRepresentation> roles) {
        managePermission.require();

        if (roles == null) {
            roles = KeycloakModelUtils.getClientScopeMappingsStream(scopedClient, scopeContainer)
                    .peek(scopeContainer::deleteScopeMapping)
                    .map(ModelToRepresentation::toBriefRepresentation)
                    .collect(Collectors.toList());
        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = scopedClient.getRole(role.getName());
                if (roleModel == null) {
                    throw new NotFoundException("Role not found");
                }
                scopeContainer.deleteScopeMapping(roleModel);
            }
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(roles).success();
    }
}
