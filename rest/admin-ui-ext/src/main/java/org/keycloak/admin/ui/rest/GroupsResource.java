package org.keycloak.admin.ui.rest;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.keycloak.common.Profile;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

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
        this.auth.groups().requireList();
        final Stream<GroupModel> stream;
        if (global) {
            stream = session.groups().searchForGroupByNameStream(realm, search.trim(), exact, first, max);
        } else {
            stream = this.realm.getTopLevelGroupsStream().filter(g -> g.getName().contains(search)).skip(first).limit(max);
        }

        return stream.map(g -> toGroupHierarchy(g, search, exact));
    }

    private GroupRepresentation toGroupHierarchy(GroupModel group, final String search, boolean exact) {
        GroupRepresentation rep = toRepresentation(group, true);
        rep.setSubGroups(group.getSubGroupsStream().filter(g ->
                ModelToRepresentation.groupMatchesSearchOrIsPathElement(g, search, false)
        ).map(subGroup ->
            ModelToRepresentation.toGroupHierarchy(
                    subGroup, true, search, exact
            )

        ).collect(Collectors.toList()));

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            setAccess(group, rep);
        }

        return rep;
    }

    // set fine-grained access for each group in the tree
    private void setAccess(GroupModel groupTree, GroupRepresentation rootGroup) {
        if (rootGroup == null) return;

        rootGroup.setAccess(auth.groups().getAccess(groupTree));

        rootGroup.getSubGroups().stream().forEach(subGroup -> {
            GroupModel foundGroupModel = groupTree.getSubGroupsStream().filter(g -> g.getId().equals(subGroup.getId())).findFirst().get();
            setAccess(foundGroupModel, subGroup);
        });

    }

}
