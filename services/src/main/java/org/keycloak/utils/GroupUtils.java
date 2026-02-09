package org.keycloak.utils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.resources.admin.fgap.GroupPermissionEvaluator;



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
    public static Stream<GroupRepresentation> populateGroupHierarchyFromSubGroups(KeycloakSession session, RealmModel realm, Stream<GroupModel> groups, boolean full, GroupPermissionEvaluator groupEvaluator, boolean subGroupsCount) {
        Map<String, GroupRepresentation> groupIdToGroups = new HashMap<>();
        groups.forEach(group -> {

            if (!AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
                //TODO GROUPS do permissions work in such a way that if you can view the children you can definitely view the parents?
                if (!groupEvaluator.canView() && !groupEvaluator.canView(group)) return;
            }

            GroupRepresentation currGroup = toRepresentation(groupEvaluator, group, full);

            if (subGroupsCount) {
                populateSubGroupCount(group, currGroup);
            }

            groupIdToGroups.putIfAbsent(currGroup.getId(), currGroup);

            while(currGroup.getParentId() != null) {
                GroupModel parentModel = session.groups().getGroupById(realm, currGroup.getParentId());

                if (!AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
                    //TODO GROUPS not sure if this is even necessary but if somehow you can't view the parent we need to remove the child and move on
                    if(!groupEvaluator.canView() && !groupEvaluator.canView(parentModel)) {
                        groupIdToGroups.remove(currGroup.getId());
                        break;
                    }                }

                GroupRepresentation parent = groupIdToGroups.computeIfAbsent(currGroup.getParentId(),
                    id -> toRepresentation(groupEvaluator, parentModel, full));

                if (subGroupsCount) {
                    populateSubGroupCount(parentModel, parent);
                }

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

    /**
     * This method's purpose is to look up the subgroup count of a Group and populate it on the representation. This has been kept separate from
     * {@link #toRepresentation} in order to keep database lookups separate from a function that aims to only convert objects
     * A way of cohesively ensuring that a GroupRepresentation always has a group count should be considered
     *
     * @param group model
     * @param representation group representation
     * @return
     */
    public static GroupRepresentation populateSubGroupCount(GroupModel group, GroupRepresentation representation) {
        representation.setSubGroupCount(group.getSubGroupsCount());
        return representation;
    }

    //From org.keycloak.admin.ui.rest.GroupsResource
    // set fine-grained access for each group in the tree
    public static GroupRepresentation toRepresentation(GroupPermissionEvaluator groupsEvaluator, GroupModel groupTree, boolean full) {
        GroupRepresentation rep = ModelToRepresentation.toRepresentation(groupTree, full);
        rep.setAccess(groupsEvaluator.getAccess(groupTree));
        return rep;
    }
}
