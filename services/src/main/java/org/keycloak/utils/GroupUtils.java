package org.keycloak.utils;

import java.util.Collections;
import java.util.stream.Collectors;

import org.keycloak.common.Profile;
import org.keycloak.models.GroupModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.resources.admin.permissions.GroupPermissionEvaluator;

public class GroupUtils {
    // Moved out from org.keycloak.admin.ui.rest.GroupsResource
    public static GroupRepresentation toGroupHierarchy(GroupPermissionEvaluator groupsEvaluator, GroupModel group, final String search, boolean exact, boolean lazy) {
        return toGroupHierarchy(groupsEvaluator, group, search, exact, true, lazy);
    }

    public static GroupRepresentation toGroupHierarchy(GroupPermissionEvaluator groupsEvaluator, GroupModel group, final String search, boolean exact, boolean full, boolean lazy) {
        GroupRepresentation rep = ModelToRepresentation.toRepresentation(group, full);
        if (!lazy) {
            rep.setSubGroups(group.getSubGroupsStream().filter(g ->
                    groupMatchesSearchOrIsPathElement(
                            g, search
                    )
            ).map(subGroup ->
                    ModelToRepresentation.toGroupHierarchy(
                            subGroup, full, search, exact
                    )

            ).collect(Collectors.toList()));
        } else {
            rep.setSubGroups(Collections.emptyList());
        }

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            setAccess(groupsEvaluator, group, rep);
        }

        return rep;
    }

    //From org.keycloak.admin.ui.rest.GroupsResource
    // set fine-grained access for each group in the tree
    private static void setAccess(GroupPermissionEvaluator groupsEvaluator, GroupModel groupTree, GroupRepresentation rootGroup) {
        if (rootGroup == null) return;

        rootGroup.setAccess(groupsEvaluator.getAccess(groupTree));

        rootGroup.getSubGroups().stream().forEach(subGroup -> {
            GroupModel foundGroupModel = groupTree.getSubGroupsStream().filter(g -> g.getId().equals(subGroup.getId())).findFirst().get();
            setAccess(groupsEvaluator, foundGroupModel, subGroup);
        });

    }

    private static boolean groupMatchesSearchOrIsPathElement(GroupModel group, String search) {
        if (StringUtil.isBlank(search)) {
            return true;
        }
        if (group.getName().contains(search)) {
            return true;
        }
        return group.getSubGroupsStream().findAny().isPresent();
    }
}
