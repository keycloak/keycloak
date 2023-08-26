package org.keycloak.admin.ui.rest;


import java.util.Objects;
import java.util.stream.Stream;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.GroupPermissionEvaluator;
import org.keycloak.utils.GroupUtils;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

public class GroupsResource {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public GroupsResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        super();
        this.realm = realm;
        this.auth = auth;
        this.session = session;
    }

    @GET
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all groups with fine grained authorisation",
            description = "This endpoint returns a list of groups with fine grained authorisation"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = GroupRepresentation.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final Stream<GroupRepresentation> listGroups(@QueryParam("search") @DefaultValue("") final String search, @QueryParam("first")
    @DefaultValue("0") int first, @QueryParam("max") @DefaultValue("10") int max, @QueryParam("global") @DefaultValue("true") boolean global,
                                                        @QueryParam("exact") @DefaultValue("false") boolean exact) {
        GroupPermissionEvaluator groupsEvaluator = auth.groups();
        groupsEvaluator.requireList();
        final Stream<GroupModel> stream;
        if (global) {
            stream = session.groups().searchForGroupByNameStream(realm, search.trim(), exact, first, max);
        } else {
            stream = this.realm.getTopLevelGroupsStream().filter(g -> g.getName().contains(search)).skip(first).limit(max);
        }

        boolean canViewGlobal = groupsEvaluator.canView();
        return stream.filter(group -> canViewGlobal || groupsEvaluator.canView(group))
                .map(group -> GroupUtils.toGroupHierarchy(groupsEvaluator, group, search, exact, "".equals(search)));
    }

    @GET
    @Path("/subgroup")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all sub groups with fine grained authorisation and pagination",
            description = "This endpoint returns a list of groups with fine grained authorisation"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = GroupRepresentation.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final Stream<GroupRepresentation> subgroups(@QueryParam("id") final String groupId, @QueryParam("search")
    @DefaultValue("") final String search, @QueryParam("first") @DefaultValue("0") int first, @QueryParam("max") @DefaultValue("10") int max) {
        GroupPermissionEvaluator groupsEvaluator = auth.groups();
        groupsEvaluator.requireList();
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            return Stream.empty();
        }

        return group.getSubGroupsStream().filter(g -> g.getName().contains(search))
                .map(g -> GroupUtils.toGroupHierarchy(groupsEvaluator, g, search, false, true)).skip(first).limit(max);
    }

    @GET
    @Path("{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "Find a specific group with no subgroups",
            description = "This endpoint returns a group by id with no subgroups"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = GroupRepresentation.class,
                            type = SchemaType.OBJECT
                    )
            )}
    )
    public GroupRepresentation findGroupById(@PathParam("id") String id) {
        GroupModel group = realm.getGroupById(id);
        this.auth.groups().requireView(group);

        GroupRepresentation rep = toRepresentation(group, true);

        rep.setAccess(auth.groups().getAccess(group));

        return rep;
    }
}
