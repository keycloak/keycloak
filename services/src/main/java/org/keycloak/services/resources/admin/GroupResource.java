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

import jakarta.ws.rs.DefaultValue;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;
import jakarta.ws.rs.NotFoundException;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.keycloak.utils.GroupUtils;

/**
 * @resource Groups
 * @author Bill Burke
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class GroupResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final GroupModel group;

    public GroupResource(RealmModel realm, GroupModel group, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.GROUP);
        this.group = group;
    }

     /**
     *
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation()
    public GroupRepresentation getGroup() {
        this.auth.groups().requireView(group);

        GroupRepresentation rep = GroupUtils.toRepresentation(this.auth.groups(), group, true);

        rep.setAccess(auth.groups().getAccess(group));

        return GroupUtils.populateSubGroupCount(group, rep);
    }

    /**
     * Update group, ignores subgroups.
     *
     * @param rep
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Update group, ignores subgroups.")
    public Response updateGroup(GroupRepresentation rep) {
        this.auth.groups().requireManage(group);

        String groupName = rep.getName();
        if (ObjectUtil.isBlank(groupName)) {
            throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
        }

        if (!Objects.equals(groupName, group.getName())) {
            boolean exists = siblings().filter(s -> !Objects.equals(s.getId(), group.getId()))
                    .anyMatch(s -> Objects.equals(s.getName(), groupName));
            if (exists) {
                throw ErrorResponse.exists("Sibling group named '" + groupName + "' already exists.");
            }
        }
        
        updateGroup(rep, group, realm, session);
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
        
        return Response.noContent().build();
    }
    
    private Stream<GroupModel> siblings() {
        if (group.getParentId() == null) {
            return session.groups().getTopLevelGroupsStream(realm);
        } else {
            return group.getParent().getSubGroupsStream();
        }
    }

    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation()
    public void deleteGroup() {
        this.auth.groups().requireManage(group);

        realm.removeGroup(group);
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    @GET
    @Path("children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Return a paginated list of subgroups that have a parent group corresponding to the group on the URL")
    public Stream<GroupRepresentation> getSubGroups(@QueryParam("first") @DefaultValue("0") Integer first,
        @QueryParam("max") @DefaultValue("10") Integer max,
        @QueryParam("briefRepresentation") @DefaultValue("false") Boolean briefRepresentation) {
        this.auth.groups().requireView(group);
        boolean canViewGlobal = auth.groups().canView();
        return group.getSubGroupsStream(first, max)
            .filter(g -> canViewGlobal || auth.groups().canView(g))
            .map(g -> GroupUtils.populateSubGroupCount(g, GroupUtils.toRepresentation(auth.groups(), g, !briefRepresentation)));
    }

    /**
     * Set or create child.  This will just set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Path("children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Set or create child.", description = "This will just set the parent if it exists. Create it and set the parent if the group doesn’t exist.")
    public Response addChild(GroupRepresentation rep) {
        this.auth.groups().requireManage(group);

        String groupName = rep.getName();
        if (ObjectUtil.isBlank(groupName)) {
            throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
        }

        try {
            Response.ResponseBuilder builder = Response.status(204);
            GroupModel child = null;
            if (rep.getId() != null) {
                child = realm.getGroupById(rep.getId());
                if (child == null) {
                    throw new NotFoundException("Could not find child by id");
                }
                if (!Objects.equals(child.getParentId(), group.getId())) {
                    realm.moveGroup(child, group);
                }
                adminEvent.operation(OperationType.UPDATE);
            } else {
                child = realm.createGroup(groupName, group);
                updateGroup(rep, child, realm, session);
                URI uri = session.getContext().getUri().getBaseUriBuilder()
                        .path(AdminRoot.class)
                        .path(AdminRoot.class, "getRealmsAdmin")
                        .path(RealmsAdminResource.class, "getRealmAdmin")
                        .path(RealmAdminResource.class, "getGroups")
                        .path(GroupsResource.class, "getGroupById")
                        .build(realm.getName(), child.getId());
                builder.status(201).location(uri);
                rep.setId(child.getId());
                adminEvent.operation(OperationType.CREATE);

            }
            adminEvent.resourcePath(session.getContext().getUri()).representation(rep).success();

            GroupRepresentation childRep = GroupUtils.toRepresentation(auth.groups(), child, true);
            return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(childRep).build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Sibling group named '" + groupName + "' already exists.");
        }
    }

    public static void updateGroup(GroupRepresentation rep, GroupModel model, RealmModel realm, KeycloakSession session) {
        String newName = rep.getName();
        if (newName != null) {
            String existingName = model.getName();
            if (!newName.equals(existingName)) {
                String previousPath = KeycloakModelUtils.buildGroupPath(model);

                model.setName(newName);

                String newPath = KeycloakModelUtils.buildGroupPath(model);

                GroupModel.GroupPathChangeEvent event =
                        new GroupModel.GroupPathChangeEvent() {
                            @Override
                            public RealmModel getRealm() {
                                return realm;
                            }

                            @Override
                            public String getNewPath() {
                                return newPath;
                            }

                            @Override
                            public String getPreviousPath() {
                                return previousPath;
                            }

                            @Override
                            public KeycloakSession getKeycloakSession() {
                                return session;
                            }
                        };
                session.getKeycloakSessionFactory().publish(event);
            }
        }

        if (rep.getAttributes() != null) {
            Set<String> attrsToRemove = new HashSet<>(model.getAttributes().keySet());
            attrsToRemove.removeAll(rep.getAttributes().keySet());
            for (Map.Entry<String, List<String>> attr : rep.getAttributes().entrySet()) {
                model.setAttribute(attr.getKey(), attr.getValue());
            }

            for (String attr : attrsToRemove) {
                model.removeAttribute(attr);
            }
        }
    }

    @Path("role-mappings")
    public RoleMapperResource getRoleMappings() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.groups().requireManage(group);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.groups().requireView(group);
        return new RoleMapperResource(session, auth, group, adminEvent, manageCheck, viewCheck);

    }

    /**
     * Get users
     *
     * Returns a stream of users, filtered according to query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param briefRepresentation Only return basic information (only guaranteed to return id, username, created, first and last name,
     *  email, enabled state, email verification state, federation link, and access.
     *  Note that it means that namely user attributes, required actions, and not before are not returned.)
     * @return a non-null {@code Stream} of users
     */
    @GET
    @NoCache
    @Path("members")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Get users Returns a stream of users, filtered according to query parameters")
    public Stream<UserRepresentation> getMembers(@Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
                                                 @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
                                                 @Parameter(description = "Only return basic information (only guaranteed to return id, username, created, first and last name, email, enabled state, email verification state, federation link, and access. Note that it means that namely user attributes, required actions, and not before are not returned.)")
                                                     @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        this.auth.groups().requireViewMembers(group);
        
        firstResult = firstResult != null ? firstResult : 0;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;

        return session.users().getGroupMembersStream(realm, group, firstResult, maxResults)
                .map(user -> briefRepresentationB
                        ? ModelToRepresentation.toBriefRepresentation(user)
                        : ModelToRepresentation.toRepresentation(session, realm, user));
    }

    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     * @return
     */
    @Path("management/permissions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Return object stating whether client Authorization permissions have been initialized or not and a reference")
    public ManagementPermissionReference getManagementPermissions() {
        auth.groups().requireView(group);

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.groups().isPermissionsEnabled(group)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(group, permissions);
    }

    public static ManagementPermissionReference toMgmtRef(GroupModel group, AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.groups().resource(group).getId());
        ref.setScopePermissions(permissions.groups().getPermissions(group));
        return ref;
    }


    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     *
     * @return initialized manage permissions reference
     */
    @Path("management/permissions")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Return object stating whether client Authorization permissions have been initialized or not and a reference")
    public ManagementPermissionReference setManagementPermissionsEnabled(ManagementPermissionReference ref) {
        auth.groups().requireManage(group);
        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.groups().setPermissionsEnabled(group, ref.isEnabled());
        if (ref.isEnabled()) {
            return toMgmtRef(group, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }

}

