package org.keycloak.utils;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
     * Functional interface for converting a GroupModel to GroupRepresentation.
     */
    @FunctionalInterface
    private interface GroupToRepresentationMapper {
        GroupRepresentation apply(GroupModel group);
    }

    /**
     * Functional interface for filtering groups based on permissions or other criteria.
     */
    @FunctionalInterface
    private interface GroupFilter {
        boolean shouldInclude(GroupModel group);
    }

    /**
     * Core implementation for building group hierarchy from subgroups.
     * This private method contains the shared logic used by both public variants.
     *
     * @param session The active keycloak session
     * @param realm The realm to operate on
     * @param groups The groups that we want to populate the hierarchy for
     * @param mapper Function to convert GroupModel to GroupRepresentation
     * @param filter Function to determine if a group should be included based on permissions
     * @param subGroupsCount Whether to populate subgroup counts
     * @return A stream of groups that contain all relevant groups from the root down with no extra siblings
     */
    private static Stream<GroupRepresentation> buildGroupHierarchy(
            KeycloakSession session,
            RealmModel realm,
            Stream<GroupModel> groups,
            GroupToRepresentationMapper mapper,
            GroupFilter filter,
            boolean subGroupsCount) {

        Map<String, GroupRepresentation> groupIdToGroups = new HashMap<>();

        groups.forEach(group -> {
            // Permission check using filter
            if (!filter.shouldInclude(group)) {
                return;
            }

            GroupRepresentation currGroup = mapper.apply(group);

            if (subGroupsCount) {
                populateSubGroupCount(group, currGroup);
            }

            groupIdToGroups.putIfAbsent(currGroup.getId(), currGroup);

            while (currGroup.getParentId() != null) {
                GroupModel parentModel = session.groups().getGroupById(realm, currGroup.getParentId());

                // Permission check for parent
                if (!filter.shouldInclude(parentModel)) {
                    groupIdToGroups.remove(currGroup.getId());
                    break;
                }

                GroupRepresentation parent = groupIdToGroups.computeIfAbsent(
                    currGroup.getParentId(),
                    id -> mapper.apply(parentModel)
                );

                if (subGroupsCount) {
                    populateSubGroupCount(parentModel, parent);
                }

                GroupRepresentation finalCurrGroup = currGroup;

                // Check the parent for existing subgroups that match the group we're currently operating on and merge them if needed
                Optional<GroupRepresentation> duplicateGroup = parent.getSubGroups() == null ?
                    Optional.empty() :
                    parent.getSubGroups().stream()
                        .filter(g -> g.equals(finalCurrGroup))
                        .findFirst();

                if (duplicateGroup.isPresent()) {
                    duplicateGroup.get().merge(currGroup);
                } else {
                    parent.getSubGroups().add(currGroup);
                }

                groupIdToGroups.remove(currGroup.getId());
                currGroup = parent;
            }
        });

        return groupIdToGroups.values().stream()
            .sorted(Comparator.comparing(GroupRepresentation::getName));
    }

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
        return buildGroupHierarchy(
            session,
            realm,
            groups,
            // Mapper with permission-aware representation
            group -> toRepresentation(groupEvaluator, group, full),
            // Filter with permission checks
            group -> {
                if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
                    return true; // FGAP v2 handles permissions differently
                }
                //TODO GROUPS do permissions work in such a way that if you can view the children you can definitely view the parents?
                return groupEvaluator.canView() || groupEvaluator.canView(group);
            },
            subGroupsCount
        );
    }

    /**
     * Simplified version of {@link #populateGroupHierarchyFromSubGroups(KeycloakSession, RealmModel, Stream, boolean, GroupPermissionEvaluator, boolean)}
     * that does not perform permission checks. Suitable for organization groups where access control is handled at the organization level.
     */
    public static Stream<GroupRepresentation> populateGroupHierarchyFromSubGroups(KeycloakSession session, RealmModel realm, Stream<GroupModel> groups, boolean full, boolean subGroupsCount) {
        return buildGroupHierarchy(
            session,
            realm,
            groups,
            // Simple mapper without permissions
            group -> full ?
                ModelToRepresentation.toRepresentation(group, true) :
                ModelToRepresentation.groupToBriefRepresentation(group),
            // No filtering - always include all groups
            group -> true,
            subGroupsCount
        );
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

    public static Set<GroupMembership> getAllMemberships(KeycloakSession session, Collection<GroupModel> groups) {
        return getAllMemberships(session, groups, true);
    }

    public static Set<GroupMembership> getAllMemberships(KeycloakSession session, Collection<GroupModel> groups, boolean direct) {
        Set<GroupMembership> memberships = new HashSet<>();

        for (GroupModel group : groups) {
            GroupMembership membership = new GroupMembership(group, direct);

            if (!memberships.add(membership)) {
                continue;
            }

            if (group.getParentId() != null) {
                RealmModel realm = session.getContext().getRealm();
                GroupModel parent = session.groups().getGroupById(realm, group.getParentId());

                if (parent != null) {
                    memberships.addAll(getAllMemberships(session, List.of(parent), false));
                }
            }
        }

        return memberships;
    }

    public record GroupMembership(GroupModel group, boolean direct) {

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof GroupMembership that)) return false;
            return Objects.equals(group, that.group);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group);
        }
    }
}
