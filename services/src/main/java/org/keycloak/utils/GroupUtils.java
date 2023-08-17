package org.keycloak.utils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.resources.admin.permissions.GroupPermissionEvaluator;



public class GroupUtils {

    /**
     * This method takes the provided groups and attempts to load their parents all the way to the root group while maintaining the hierarchy data
     * for each GroupRepresentation object. Each resultant GroupRepresentation object in the stream should contain relevant subgroups to the originally
     * provided groups
     * @param session The active keycloak session
     * @param realm The realm to operate on
     * @param groups The groups that we want to populate the hierarchy for
     * @return A stream of groups that contain all relevant groups from the root down with no extra siblings
     */
    public static Stream<GroupRepresentation> populateGroupHierarchyFromSubGroups(KeycloakSession session, RealmModel realm, Stream<GroupModel> groups, boolean full, GroupPermissionEvaluator groupEvaluator) {
        Map<String, GroupRepresentation> groupIdToGroups = new HashMap<>();
        groups.forEach(group -> {
            // TODO GROUPS do permissions work in such a way that if you can view the children you can definitely view the parents?
            if(!groupEvaluator.canView() && !groupEvaluator.canView(group)) return;

            GroupRepresentation currGroup = ModelToRepresentation.toRepresentation(group, full);
            populateSubGroupCount(realm, session, currGroup);
            groupIdToGroups.putIfAbsent(currGroup.getId(), currGroup);

            while(currGroup.getParentId() != null) {
                GroupModel parentModel = session.groups().getGroupById(realm, currGroup.getParentId());

                // TODO GROUPS not sure if this is even necessary but if somehow you can't view the parent we need to remove the child and move on
                if(!groupEvaluator.canView() && !groupEvaluator.canView(parentModel)) {
                    groupIdToGroups.remove(currGroup.getId());
                    break;
                }

                GroupRepresentation parent = groupIdToGroups.computeIfAbsent(currGroup.getParentId(),
                    id -> ModelToRepresentation.toRepresentation(parentModel, full));
                // TODO GROUPS this is here but it really could be moved to be part of converting a model to a representation.
                populateSubGroupCount(realm, session, parent);
                GroupRepresentation finalCurrGroup = currGroup;

                // check the parent for existing subgroups that match the group we're currently operating on and merge them if needed
                Optional<GroupRepresentation> duplicateGroup = parent.getSubGroups() == null ?
                    Optional.empty() : parent.getSubGroups().stream().filter(g -> g.equals(finalCurrGroup)).findFirst();
                if(duplicateGroup.isPresent()) {
                    duplicateGroup.get().merge(currGroup);
                } else {
                    parent.getSubGroups().add(currGroup);
                }
                groupIdToGroups.remove(currGroup.getId());
                currGroup = parent;
            }
        });
        return groupIdToGroups.values().stream().sorted(Comparator.comparing(GroupRepresentation::getName));
    }

    public static GroupRepresentation populateSubGroupCount(RealmModel realm, KeycloakSession session, GroupRepresentation representation) {
        representation.setSubGroupCount(session.groups().getSubGroupsCount(realm, representation.getId()));
        return representation;
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
