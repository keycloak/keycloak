package org.keycloak.utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static Stream<GroupRepresentation> toAncestorsLine(GroupPermissionEvaluator groupsEvaluator, Stream<GroupModel> stream) {
        List<GroupRepresentationExtended> tree = new ArrayList<>();
        HashMap<String,GroupRepresentationExtended> groupMap = new HashMap<>();

        stream.forEach(g ->  {
            getAncestryStream(groupsEvaluator, g).forEach(group -> {
                GroupRepresentationExtended alreadyProcessedGroup = groupMap.get( group.getGroupRep().getId());
                String parentId = group.getParentId();
                if (parentId == null) {
                    if(alreadyProcessedGroup == null || !tree.contains(alreadyProcessedGroup)) {
                        tree.add(group);
                        groupMap.put(group.getGroupRep().getId(), group);
                    } else if (alreadyProcessedGroup != null) {
                        // already processed a top level group, do nothing
                    }
                } else {
                    GroupRepresentationExtended foundParent  = groupMap.get(parentId);
                    if ( foundParent.getGroupRep().getSubGroups() == null) {
                        foundParent.getGroupRep().setSubGroups(new ArrayList<>());
                    }
                    if (groupMap.get(group.getGroupRep().getId()) == null) {
                        foundParent.getGroupRep().getSubGroups().add(group.getGroupRep());
                        groupMap.put(group.getGroupRep().getId(), group);
                    }
                }
            } );
        });
        return tree.stream().map(GroupRepresentationExtended::getGroupRep).collect(Collectors.toList()).stream();
    }

    private static class GroupRepresentationExtended {
        private String parentId;

        public GroupRepresentation getGroupRep() {
            return groupRep;
        }

        private GroupRepresentation groupRep;

        public GroupRepresentationExtended(GroupRepresentation group, String parentId) {
            this.groupRep = group;
            this.parentId = parentId;
        }

        public String getParentId() {
            return parentId;
        }

    }
    private static Stream<GroupRepresentationExtended> getAncestryStream(GroupPermissionEvaluator groupsEvaluator, GroupModel group) {
        List<GroupRepresentationExtended> groupsList = new ArrayList<>();
        GroupModel currentGroup = group;
        while (currentGroup != null) {
            Map<String, Boolean> access =  groupsEvaluator.getAccess(currentGroup);
            GroupRepresentation groupRepresentation = ModelToRepresentation.toRepresentation(currentGroup, false);
            groupRepresentation.setAccess(access);
            groupsList.add(new GroupRepresentationExtended(groupRepresentation, currentGroup.getParentId()));
            currentGroup =  currentGroup.getParent();;
        }
        Collections.reverse(groupsList);
        return groupsList.stream();
    }

}
