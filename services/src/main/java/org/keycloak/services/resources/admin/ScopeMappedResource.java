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
import java.util.Map;
import java.util.Objects;
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
import jakarta.ws.rs.PathParam;
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
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.util.ScopeMappedUtil;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

/**
 * Base class for managing the scope mappings of a specific client.
 *
 * @resource Scope Mappings
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ScopeMappedResource {
    protected RealmModel realm;
    protected AdminPermissionEvaluator auth;
    protected AdminPermissionEvaluator.RequirePermissionCheck managePermission;
    protected AdminPermissionEvaluator.RequirePermissionCheck viewPermission;

    protected ScopeContainerModel scopeContainer;
    protected KeycloakSession session;
    protected AdminEventBuilder adminEvent;

    public ScopeMappedResource(RealmModel realm, AdminPermissionEvaluator auth, ScopeContainerModel scopeContainer,
                               KeycloakSession session, AdminEventBuilder adminEvent,
                               AdminPermissionEvaluator.RequirePermissionCheck managePermission,
                               AdminPermissionEvaluator.RequirePermissionCheck viewPermission) {
        this.realm = realm;
        this.auth = auth;
        this.scopeContainer = scopeContainer;
        this.session = session;
        this.adminEvent = adminEvent.resource(ResourceType.REALM_SCOPE_MAPPING);
        this.managePermission = managePermission;
        this.viewPermission = viewPermission;
    }

    /**
     * Get all scope mappings for the client
     *
     * @return
     * @deprecated the method is not used neither from admin console or from admin client. It may be removed in future releases.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Deprecated
    @Tag(name= KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Get all scope mappings for the client", deprecated = true)
    public MappingsRepresentation getScopeMappings() {
        viewPermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        MappingsRepresentation all = new MappingsRepresentation();
        List<RoleRepresentation> realmRep = scopeContainer.getRealmScopeMappingsStream()
                .map(ModelToRepresentation::toBriefRepresentation)
                .collect(Collectors.toList());
        if (!realmRep.isEmpty()) {
            all.setRealmMappings(realmRep);
        }

        Stream<ClientModel> clients = realm.getClientsStream();
        Map<String, ClientMappingsRepresentation> clientMappings = clients
                .map(c -> ScopeMappedUtil.toClientMappingsRepresentation(c, scopeContainer))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ClientMappingsRepresentation::getClient, Function.identity()));

        if (!clientMappings.isEmpty()) {
            all.setClientMappings(clientMappings);
        }
        return all;
    }

    /**
     * Get realm-level roles associated with the client's scope
     *
     * @return
     */
    @Path("realm")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Get realm-level roles associated with the client's scope")
    public Stream<RoleRepresentation> getRealmScopeMappings() {
        viewPermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        return scopeContainer.getRealmScopeMappingsStream()
                .map(ModelToRepresentation::toBriefRepresentation);
    }

    /**
     * Get realm-level roles that are available to attach to this client's scope
     *
     * @return
     */
    @Path("realm/available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Get realm-level roles that are available to attach to this client's scope")
    public Stream<RoleRepresentation> getAvailableRealmScopeMappings() {
        viewPermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        return realm.getRolesStream()
                .filter(((Predicate<RoleModel>) scopeContainer::hasDirectScope).negate())
                .filter(auth.roles()::canMapClientScope)
                .map(ModelToRepresentation::toBriefRepresentation);
    }

    /**
     * Get effective realm-level roles associated with the client's scope
     *
     * What this does is recurse
     * any composite roles associated with the client's scope and adds the roles to this lists.  The method is really
     * to show a comprehensive total view of realm-level roles associated with the client.
     *
     * @param briefRepresentation if false, return roles with their attributes
     * 
     * @return
     */
    @Path("realm/composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Get effective realm-level roles associated with the client’s scope What this does is recurse any composite roles associated with the client’s scope and adds the roles to this lists.",
        description = "The method is really to show a comprehensive total view of realm-level roles associated with the client.")
    public Stream<RoleRepresentation> getCompositeRealmScopeMappings(@Parameter(description = "if false, return roles with their attributes") @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        viewPermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        Function<RoleModel, RoleRepresentation> toBriefRepresentation = briefRepresentation ?
                ModelToRepresentation::toBriefRepresentation : ModelToRepresentation::toRepresentation;
        return realm.getRolesStream()
                .filter(scopeContainer::hasScope)
                .map(toBriefRepresentation);
    }

    /**
     * Add a set of realm-level roles to the client's scope
     *
     * @param roles
     */
    @Path("realm")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Add a set of realm-level roles to the client's scope")
    @APIResponse(responseCode = "204", description = "No Content")
    public void addRealmScopeMappings(List<RoleRepresentation> roles) {
        managePermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRoleById(role.getId());
            if (roleModel == null) {
                throw new NotFoundException("Role not found");
            }
            scopeContainer.addScopeMapping(roleModel);
        }

        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(roles).success();
    }

    /**
     * Remove a set of realm-level roles from the client's scope
     *
     * @param roles
     */
    @Path("realm")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SCOPE_MAPPINGS)
    @Operation(summary = "Remove a set of realm-level roles from the client's scope")
    public void deleteRealmScopeMappings(List<RoleRepresentation> roles) {
        managePermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        if (roles == null) {
            roles = scopeContainer.getRealmScopeMappingsStream()
                    .peek(scopeContainer::deleteScopeMapping)
                    .map(ModelToRepresentation::toBriefRepresentation)
                    .collect(Collectors.toList());
       } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRoleById(role.getId());
                if (roleModel == null) {
                    throw new NotFoundException("Role not found");
                }
                scopeContainer.deleteScopeMapping(roleModel);
            }
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(roles).success();

    }

    @Path("clients/{client}")
    public ScopeMappedClientResource getClientByIdScopeMappings(@PathParam("client") String client) {
        ClientModel clientModel = realm.getClientById(client);
        if (clientModel == null) {
            throw new NotFoundException("Could not find client");
        }
        return new ScopeMappedClientResource(realm, auth, this.scopeContainer, session, clientModel, adminEvent, managePermission, viewPermission);
    }
}
