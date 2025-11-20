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

package org.keycloak.storage.ldap.mappers.membership.group;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapper;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.UserRolesRetrieveStrategy;
import org.keycloak.storage.user.SynchronizationResult;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupLDAPStorageMapper extends AbstractLDAPStorageMapper implements CommonLDAPGroupMapper {

    private static final Logger logger = Logger.getLogger(GroupLDAPStorageMapper.class);

    private final GroupMapperConfig config;
    private final GroupLDAPStorageMapperFactory factory;

    // Flag to avoid syncing multiple times per transaction
    private boolean syncFromLDAPPerformedInThisTransaction = false;

    public GroupLDAPStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, GroupLDAPStorageMapperFactory factory) {
        super(mapperModel, ldapProvider);
        this.config = new GroupMapperConfig(mapperModel);
        this.factory = factory;
    }


    // CommonLDAPGroupMapper interface

    @Override
    public LDAPQuery createLDAPGroupQuery() {
        return createGroupQuery(false);
    }

    @Override
    public CommonLDAPGroupMapperConfig getConfig() {
        return config;
    }



    // LDAP Group CRUD operations
    // !! This function must be always called from try-with-resources block, otherwise vault secret may be leaked !!
    public LDAPQuery createGroupQuery(boolean includeMemberAttribute) {
        LDAPQuery ldapQuery = new LDAPQuery(ldapProvider);

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapQuery.setSearchScope(ldapProvider.getLdapIdentityStore().getConfig().getSearchScope());

        String groupsDn = config.getGroupsDn();
        ldapQuery.setSearchDn(groupsDn);

        Collection<String> groupObjectClasses = config.getGroupObjectClasses(ldapProvider);
        ldapQuery.addObjectClasses(groupObjectClasses);

        String customFilter = config.getCustomLdapFilter();
        if (customFilter != null && customFilter.trim().length() > 0) {
            Condition customFilterCondition = new LDAPQueryConditionsBuilder().addCustomLDAPFilter(customFilter);
            ldapQuery.addWhereCondition(customFilterCondition);
        }

        ldapQuery.addReturningLdapAttribute(config.getGroupNameLdapAttribute());

        // Performance improvement
        if (includeMemberAttribute) {
            ldapQuery.addReturningLdapAttribute(config.getMembershipLdapAttribute());
        }

        for (String groupAttr : config.getGroupAttributes()) {
            ldapQuery.addReturningLdapAttribute(groupAttr);
        }

        return ldapQuery;
    }

    public LDAPObject createLDAPGroup(String groupName, Map<String, Set<String>> additionalAttributes) {
        LDAPObject ldapGroup = LDAPUtils.createLDAPGroup(ldapProvider, groupName, config.getGroupNameLdapAttribute(), config.getGroupObjectClasses(ldapProvider),
                config.getRelativeCreateDn() + config.getGroupsDn(), additionalAttributes, config.getMembershipLdapAttribute());

        logger.debugf("Creating group [%s] to LDAP with DN [%s]", groupName, ldapGroup.getDn().toString());
        return ldapGroup;
    }

    public LDAPObject loadLDAPGroupByName(String groupName) {
        try (LDAPQuery ldapQuery = createGroupQuery(true)) {
            Condition roleNameCondition = new LDAPQueryConditionsBuilder().equal(config.getGroupNameLdapAttribute(), groupName);
            ldapQuery.addWhereCondition(roleNameCondition);
            return ldapQuery.getFirstResult();
        }
    }

    public LDAPObject updateLDAPGroup(LDAPObject ldapObject) {
        LDAPObject ldapGroup = LDAPUtils.updateLDAPGroup(ldapProvider, ldapObject);

        return ldapGroup;
    }

    protected Set<LDAPDn> getLDAPSubgroups(LDAPObject ldapGroup) {
        MembershipType membershipType = config.getMembershipTypeLdapAttribute();
        return membershipType.getLDAPSubgroups(this, ldapGroup);
    }


    // Sync from Ldap to KC

    @Override
    public SynchronizationResult syncDataFromFederationProviderToKeycloak(RealmModel realm) {
        SynchronizationResult syncResult = new SynchronizationResult() {

            @Override
            public String getStatus() {
                return String.format("%d imported groups, %d updated groups, %d removed groups", getAdded(), getUpdated(), getRemoved());
            }

        };

        logger.debugf("Syncing groups from LDAP into Keycloak DB. Mapper is [%s], LDAP provider is [%s]", mapperModel.getName(), ldapProvider.getModel().getName());

        // Get all LDAP groups
        List<LDAPObject> ldapGroups = getAllLDAPGroups(config.isPreserveGroupsInheritance());

        // Convert to internal format
        Map<String, LDAPObject> ldapGroupsMap = new HashMap<>();
        List<GroupTreeResolver.Group> ldapGroupsRep = new LinkedList<>();
        convertGroupsToInternalRep(ldapGroups, ldapGroupsMap, ldapGroupsRep);

        // Now we have list of LDAP groups. Let's form the tree (if needed)
        if (config.isPreserveGroupsInheritance()) {
            try {
                List<GroupTreeResolver.GroupTreeEntry> groupTrees = new GroupTreeResolver().resolveGroupTree(ldapGroupsRep, config.isIgnoreMissingGroups());

                updateKeycloakGroupTree(realm, groupTrees, ldapGroupsMap, syncResult);
            } catch (GroupTreeResolver.GroupTreeResolveException gre) {
                throw new ModelException("Couldn't resolve groups from LDAP. Fix LDAP or skip preserve inheritance. Details: " + gre.getMessage(), gre);
            }
        } else {
            syncFlatGroupStructure(realm, syncResult, ldapGroupsMap);
        }

        syncFromLDAPPerformedInThisTransaction = true;

        return syncResult;
    }

    private void syncExistingGroup(RealmModel realm, GroupModel kcExistingGroup, Map.Entry<String, LDAPObject> groupEntry,
                                   SynchronizationResult syncResult, Set<String> visitedGroupIds, String groupName) {
        try {
            // Update each existing group to be synced in its own inner transaction to prevent race condition when
            // the groups intended to be updated was already deleted via other channel in the meantime
            KeycloakModelUtils.runJobInTransaction(ldapProvider.getSession().getKeycloakSessionFactory(), session -> {
                RealmModel innerTransactionRealm = session.realms().getRealm(realm.getId());
                GroupModel innerTransactionGroup = session.groups().getGroupById(innerTransactionRealm, kcExistingGroup.getId());
                updateAttributesOfKCGroup(innerTransactionGroup, groupEntry.getValue());
                syncResult.increaseUpdated();
                visitedGroupIds.add(kcExistingGroup.getId());
            });

        } catch (ModelException me) {
            logger.error(String.format("Failed to update attributes of LDAP group %s: ", groupName), me);
            syncResult.increaseFailed();
        }
    }

    private void syncNonExistingGroup(RealmModel realm, Map.Entry<String, LDAPObject> groupEntry,
                                      SynchronizationResult syncResult, Set<String> visitedGroupIds, String groupName) {
        try {
            // Create each non-existing group to be synced in its own inner transaction to prevent race condition when
            // the group intended to be created was already created via other channel in the meantime
            KeycloakModelUtils.runJobInTransaction(ldapProvider.getSession().getKeycloakSessionFactory(), session -> {
                RealmModel innerTransactionRealm = session.realms().getRealm(realm.getId());
                GroupModel kcGroup = createKcGroup(innerTransactionRealm, groupName, null);
                updateAttributesOfKCGroup(kcGroup, groupEntry.getValue());
                syncResult.increaseAdded();
                visitedGroupIds.add(kcGroup.getId());
            });
        } catch (ModelException me) {
            logger.error(String.format("Failed to sync group %s from LDAP: ", groupName), me);
            syncResult.increaseFailed();
        }
    }

    private void convertGroupsToInternalRep(List<LDAPObject> ldapGroups, Map<String, LDAPObject> ldapGroupsMap,
                                            List<GroupTreeResolver.Group> ldapGroupsRep) {
        String groupsRdnAttr = config.getGroupNameLdapAttribute();
        for (LDAPObject ldapGroup : ldapGroups) {
            String groupName = ldapGroup.getAttributeAsString(groupsRdnAttr);
            //String groupName = ldapGroup.getUuid();

            if (config.isPreserveGroupsInheritance()) {
                Set<String> subgroupNames = new HashSet<>();
                for (LDAPDn groupDn : getLDAPSubgroups(ldapGroup)) {
                    String subGroupName = groupDn.getFirstRdn().getAttrValue(groupsRdnAttr);
                    subgroupNames.add(subGroupName);
                }

                ldapGroupsRep.add(new GroupTreeResolver.Group(groupName, subgroupNames));
            }

            ldapGroupsMap.put(groupName, ldapGroup);
        }
    }

    private void syncFlatGroupStructure(RealmModel realm, SynchronizationResult syncResult, Map<String, LDAPObject> ldapGroupsMap) {
        Set<String> visitedGroupIds = new HashSet<>();

        // Just add flat structure of groups with all groups at groups path
        LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();
        final int groupsPerTransaction = ldapConfig.getBatchSizeForSync();
        Set<Map.Entry<String, LDAPObject>> entries = ldapGroupsMap.entrySet();
        for (Iterator<Map.Entry<String, LDAPObject>> it = entries.iterator(); it.hasNext(); ) {

            KeycloakModelUtils.runJobInTransaction(ldapProvider.getSession().getKeycloakSessionFactory(), session -> {

                // KEYCLOAK-8253 The retrieval of the current realm to operate at, was intentionally left
                // outside the following for loop! This prevents the scenario, when LDAP group sync time
                // initially improves, but during the time (after ~20K groups are synced) degrades again
                // due to the realm cache being bloated with huge amount of (temporary) realm entities
                RealmModel currentRealm = session.realms().getRealm(realm.getId());

                // List of group path groups known to the whole transaction
                Map<String, GroupModel> transactionGroupPathGroups = getKcSubGroups(currentRealm, null)
                        .collect(Collectors.toMap(GroupModel::getName, Function.identity()));

                for (int i = 0; i < groupsPerTransaction && it.hasNext(); i++) {
                    Map.Entry<String, LDAPObject> groupEntry = it.next();

                    String groupName = groupEntry.getKey();
                    GroupModel kcExistingGroup = transactionGroupPathGroups.get(groupName);

                    if (kcExistingGroup != null) {
                        syncExistingGroup(currentRealm, kcExistingGroup, groupEntry, syncResult, visitedGroupIds, groupName);
                    } else {
                        syncNonExistingGroup(currentRealm, groupEntry, syncResult, visitedGroupIds, groupName);
                    }
                }
            });
        }

        // Possibly remove keycloak groups, which don't exist in LDAP
        if (config.isDropNonExistingGroupsDuringSync()) {
            dropNonExistingKcGroups(realm, syncResult, visitedGroupIds);
        }
    }

    private void updateKeycloakGroupTree(RealmModel realm, List<GroupTreeResolver.GroupTreeEntry> groupTrees, Map<String, LDAPObject> ldapGroups, SynchronizationResult syncResult) {
        Set<String> visitedGroupIds = new HashSet<>();

        for (GroupTreeResolver.GroupTreeEntry groupEntry : groupTrees) {
            updateKeycloakGroupTreeEntry(realm, groupEntry, ldapGroups, null, syncResult, visitedGroupIds);
        }

        // Possibly remove keycloak groups, which don't exist in LDAP
        if (config.isDropNonExistingGroupsDuringSync()) {
            dropNonExistingKcGroups(realm, syncResult, visitedGroupIds);
        }
    }

    private void updateKeycloakGroupTreeEntry(RealmModel realm, GroupTreeResolver.GroupTreeEntry groupTreeEntry, Map<String, LDAPObject> ldapGroups, GroupModel kcParent, SynchronizationResult syncResult, Set<String> visitedGroupIds) {
        String groupName = groupTreeEntry.getGroupName();

        // Check if group already exists
        GroupModel kcGroup = getKcSubGroups(realm, kcParent)
                .filter(g -> Objects.equals(g.getName(), groupName)).findFirst().orElse(null);

        if (kcGroup != null) {
            logger.debugf("Updated Keycloak group '%s' from LDAP", kcGroup.getName());
            updateAttributesOfKCGroup(kcGroup, ldapGroups.get(kcGroup.getName()));
            syncResult.increaseUpdated();
        } else {
            kcGroup = createKcGroup(realm, groupTreeEntry.getGroupName(), kcParent);
            if (kcGroup.getParent() == null) {
                logger.debugf("Imported top-level group '%s' from LDAP", kcGroup.getName());
            } else {
                logger.debugf("Imported group '%s' from LDAP as child of group '%s'", kcGroup.getName(), kcGroup.getParent().getName());
            }

            updateAttributesOfKCGroup(kcGroup, ldapGroups.get(kcGroup.getName()));
            syncResult.increaseAdded();
        }

        visitedGroupIds.add(kcGroup.getId());

        for (GroupTreeResolver.GroupTreeEntry childEntry : groupTreeEntry.getChildren()) {
            updateKeycloakGroupTreeEntry(realm, childEntry, ldapGroups, kcGroup, syncResult, visitedGroupIds);
        }
    }

    private void dropNonExistingKcGroups(RealmModel realm, SynchronizationResult syncResult, Set<String> visitedGroupIds) {
        // Remove keycloak groups, which don't exist in LDAP
        GroupModel parent = getKcGroupsPathGroup(realm);

        getAllKcGroups(realm, parent)
                .filter(kcGroup -> !visitedGroupIds.contains(kcGroup.getId()))
                .forEach(kcGroup -> {
                    logger.debugf("Removing Keycloak group '%s', which doesn't exist in LDAP", kcGroup.getName());
                    realm.removeGroup(kcGroup);
                    syncResult.increaseRemoved();
                });
    }

    private void updateAttributesOfKCGroup(GroupModel kcGroup, LDAPObject ldapGroup) {
        Collection<String> groupAttributes = config.getGroupAttributes();

        for (String attrName : groupAttributes) {
            Set<String> attrValues = ldapGroup.getAttributeAsSet(attrName);
            if (attrValues==null) {
                kcGroup.removeAttribute(attrName);
            } else {
                kcGroup.setAttribute(attrName, new LinkedList<>(attrValues));
            }
        }
    }


    protected GroupModel findKcGroupByLDAPGroup(RealmModel realm, GroupModel parent, LDAPObject ldapGroup) {
        String groupNameAttr = config.getGroupNameLdapAttribute();
        String groupName = ldapGroup.getAttributeAsString(groupNameAttr);

        if (config.isPreserveGroupsInheritance()) {
            // Override if better effectivity or different algorithm is needed
            return getAllKcGroups(realm, parent)
                    .filter(group -> Objects.equals(group.getName(), groupName)).findFirst().orElse(null);
        } else {
            // Without preserved inheritance, it's always at groups path
            return getSession().groups().getGroupByName(realm, parent, groupName);
        }
    }

    protected GroupModel findKcGroupOrSyncFromLDAP(RealmModel realm, GroupModel parent, LDAPObject ldapGroup, UserModel user) {
        GroupModel kcGroup = findKcGroupByLDAPGroup(realm, parent, ldapGroup);

        if (kcGroup == null) {

            if (config.isPreserveGroupsInheritance()) {

                // Better to sync all groups from LDAP with preserved inheritance
                if (!syncFromLDAPPerformedInThisTransaction) {
                    syncDataFromFederationProviderToKeycloak(realm);
                    kcGroup = findKcGroupByLDAPGroup(realm, parent, ldapGroup);
                }
            } else {
                String groupNameAttr = config.getGroupNameLdapAttribute();
                String groupName = ldapGroup.getAttributeAsString(groupNameAttr);

                kcGroup = createKcGroup(realm, groupName, null);
                updateAttributesOfKCGroup(kcGroup, ldapGroup);
            }

            // Could theoretically happen on some LDAP servers if 'memberof' style is used and 'memberof' attribute of user references non-existing group
            if (kcGroup == null) {
                String groupName = ldapGroup.getAttributeAsString(config.getGroupNameLdapAttribute());
                logger.warnf("User '%s' is member of group '%s', which doesn't exist in LDAP", user.getUsername(), groupName);
            }
        }

        return kcGroup;
    }

    // Send LDAP query to retrieve all groups
    protected List<LDAPObject> getAllLDAPGroups(boolean includeMemberAttribute) {
        try (LDAPQuery ldapGroupQuery = createGroupQuery(includeMemberAttribute)) {
            return LDAPUtils.loadAllLDAPObjects(ldapGroupQuery, ldapProvider);
        }
    }


    // Sync from Keycloak to LDAP
    @Override
    public SynchronizationResult syncDataFromKeycloakToFederationProvider(RealmModel realm) {
        SynchronizationResult syncResult = new SynchronizationResult() {

            @Override
            public String getStatus() {
                return String.format("%d groups imported to LDAP, %d groups updated to LDAP, %d groups removed from LDAP", getAdded(), getUpdated(), getRemoved());
            }

        };

        if (config.getMode() != LDAPGroupMapperMode.LDAP_ONLY) {
            logger.warnf("Ignored sync for federation mapper '%s' as it's mode is '%s'", mapperModel.getName(), config.getMode().toString());
            return syncResult;
        }

        logger.debugf("Syncing groups from Keycloak into LDAP. Mapper is [%s], LDAP provider is [%s]", mapperModel.getName(), ldapProvider.getModel().getName());

        // Query existing LDAP groups

        List<LDAPObject> ldapGroups = getAllLDAPGroups(config.isPreserveGroupsInheritance());

        // Convert them to Map<String, LDAPObject>
        Map<String, LDAPObject> ldapGroupsMap = new HashMap<>();
        String groupsRdnAttr = config.getGroupNameLdapAttribute();
        for (LDAPObject ldapGroup : ldapGroups) {
            String groupName = ldapGroup.getAttributeAsString(groupsRdnAttr);
            ldapGroupsMap.put(groupName, ldapGroup);
        }


        // Map to track all LDAP groups also exist in Keycloak
        Set<String> ldapGroupNames = new HashSet<>();

        // Create or update KC groups to LDAP including their attributes
        getKcSubGroups(realm, null)
                .forEach(kcGroup -> processKeycloakGroupSyncToLDAP(kcGroup, ldapGroupsMap, ldapGroupNames, syncResult));

        // If dropNonExisting, then drop all groups, which doesn't exist in KC from LDAP as well
        if (config.isDropNonExistingGroupsDuringSync()) {
            Set<String> copy = new HashSet<>(ldapGroupsMap.keySet());
            for (String groupName : copy) {
                if (!ldapGroupNames.contains(groupName)) {
                    LDAPObject ldapGroup = ldapGroupsMap.remove(groupName);
                    ldapProvider.getLdapIdentityStore().remove(ldapGroup);
                    syncResult.increaseRemoved();
                }
            }
        }

        // Finally process memberships,
        if (config.isPreserveGroupsInheritance()) {
            getKcSubGroups(realm, null)
                    .forEach(kcGroup -> processKeycloakGroupMembershipsSyncToLDAP(kcGroup, ldapGroupsMap));
        }

        return syncResult;
    }

    // For given kcGroup check if it exists in LDAP (map) by name
    // If not, create it in LDAP including attributes. Otherwise update attributes in LDAP.
    // Process this recursively for all subgroups of KC group
    private void processKeycloakGroupSyncToLDAP(GroupModel kcGroup, Map<String, LDAPObject> ldapGroupsMap, Set<String> ldapGroupNames, SynchronizationResult syncResult) {
        String groupName = kcGroup.getName();

        // extract group attributes to be updated to LDAP
        Map<String, Set<String>> supportedLdapAttributes = new HashMap<>();
        for (String attrName : config.getGroupAttributes()) {
            Set<String> valueSet = kcGroup.getAttributeStream(attrName).collect(Collectors.toSet());
            supportedLdapAttributes.put(attrName, valueSet.isEmpty() ? null : valueSet);
        }

        LDAPObject ldapGroup = ldapGroupsMap.get(groupName);

        if (ldapGroup == null) {
            ldapGroup = createLDAPGroup(groupName, supportedLdapAttributes);
            syncResult.increaseAdded();
        } else {
            for (Map.Entry<String, Set<String>> attrEntry : supportedLdapAttributes.entrySet()) {
                ldapGroup.setAttribute(attrEntry.getKey(), attrEntry.getValue());
            }

            ldapProvider.getLdapIdentityStore().update(ldapGroup);
            syncResult.increaseUpdated();
        }

        ldapGroupsMap.put(groupName, ldapGroup);
        ldapGroupNames.add(groupName);

        // process KC subgroups
        kcGroup.getSubGroupsStream()
                .forEach(kcSubgroup -> processKeycloakGroupSyncToLDAP(kcSubgroup, ldapGroupsMap, ldapGroupNames, syncResult));
    }

    // Update memberships of group in LDAP based on subgroups from KC. Do it recursively
    private void processKeycloakGroupMembershipsSyncToLDAP(GroupModel kcGroup, Map<String, LDAPObject> ldapGroupsMap) {
        LDAPObject ldapGroup = ldapGroupsMap.get(kcGroup.getName());
        Set<LDAPDn> toRemoveSubgroupsDNs = getLDAPSubgroups(ldapGroup);

        String membershipUserLdapAttrName = getMembershipUserLdapAttribute(); // Not applicable for groups, but needs to be here

        // Add LDAP subgroups, which are KC subgroups
        Set<GroupModel> kcSubgroups = kcGroup.getSubGroupsStream().collect(Collectors.toSet());
        for (GroupModel kcSubgroup : kcSubgroups) {
            LDAPObject ldapSubgroup = ldapGroupsMap.get(kcSubgroup.getName());
            if (!toRemoveSubgroupsDNs.remove(ldapSubgroup.getDn())) {
                // if the group is not in the ldap group => add it
                LDAPUtils.addMember(ldapProvider, MembershipType.DN, config.getMembershipLdapAttribute(), membershipUserLdapAttrName, ldapGroup, ldapSubgroup);
            }
        }

        // Remove LDAP subgroups, which are not members in KC anymore
        for (LDAPDn toRemoveDN : toRemoveSubgroupsDNs) {
            LDAPObject fakeGroup = new LDAPObject();
            fakeGroup.setDn(toRemoveDN);
            LDAPUtils.deleteMember(ldapProvider, MembershipType.DN, config.getMembershipLdapAttribute(), membershipUserLdapAttrName, ldapGroup, fakeGroup);
        }

        for (GroupModel kcSubgroup : kcSubgroups) {
            processKeycloakGroupMembershipsSyncToLDAP(kcSubgroup, ldapGroupsMap);
        }
    }

    // Recursively check if parent group exists in LDAP. If yes, then return current group. If not, then recursively call this method
    // for the predecessor. Result is the highest group, which doesn't yet exist in LDAP (and hence requires sync to LDAP)
    private GroupModel getHighestPredecessorNotExistentInLdap(GroupModel groupsPathGroup, GroupModel group) {
        GroupModel parentGroup = group.getParent();
        if (parentGroup == groupsPathGroup) {
            return group;
        }

        LDAPObject ldapGroup = loadLDAPGroupByName(parentGroup.getName());
        if (ldapGroup != null) {
            // Parent exists in LDAP. Let's return current group
            return group;
        } else {
            // Parent doesn't exist in LDAP. Let's recursively go up.
            return getHighestPredecessorNotExistentInLdap(groupsPathGroup, parentGroup);
        }
    }


    // group-user membership operations


    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel kcGroup, int firstResult, int maxResults) {
        if (config.getMode() == LDAPGroupMapperMode.IMPORT) {
            // only results from Keycloak should be returned, or imported LDAP and KC items will duplicate
            return Collections.emptyList();
        }

        if (!isGroupInGroupPath(realm, kcGroup)) {
            // group being inspected is not managed by this mapper - return empty collection
            return Collections.emptyList();
        }

        // TODO: with ranged search in AD we can improve the search using the specific range (not done for the moment)
        LDAPObject ldapGroup = loadLDAPGroupByName(kcGroup.getName());
        if (ldapGroup == null) {
            return Collections.emptyList();
        }

        String strategyKey = config.getUserGroupsRetrieveStrategy();
        UserRolesRetrieveStrategy strategy = factory.getUserGroupsRetrieveStrategy(strategyKey);
        return strategy.getLDAPRoleMembers(realm, this, ldapGroup, firstResult, maxResults);
    }

    public void addGroupMappingInLDAP(RealmModel realm, GroupModel kcGroup, LDAPObject ldapUser) {
        String groupName = kcGroup.getName();
        LDAPObject ldapGroup = loadLDAPGroupByName(groupName);

        if (ldapGroup == null) {
            // Needs to partially sync Keycloak groups to LDAP
            if (config.isPreserveGroupsInheritance()) {
                GroupModel groupsPathGroup = getKcGroupsPathGroup(realm);
                GroupModel highestGroupToSync = getHighestPredecessorNotExistentInLdap(groupsPathGroup, kcGroup);

                logger.debugf("Will sync group '%s' and it's subgroups from DB to LDAP", highestGroupToSync.getName());

                Map<String, LDAPObject> syncedLDAPGroups = new HashMap<>();
                processKeycloakGroupSyncToLDAP(highestGroupToSync, syncedLDAPGroups, new HashSet<>(), new SynchronizationResult());
                processKeycloakGroupMembershipsSyncToLDAP(highestGroupToSync, syncedLDAPGroups);

                ldapGroup = loadLDAPGroupByName(groupName);

                // Finally update LDAP membership in the parent group
                if (highestGroupToSync.getParent() != groupsPathGroup) {
                    LDAPObject ldapParentGroup = loadLDAPGroupByName(highestGroupToSync.getParent().getName());
                    LDAPUtils.addMember(ldapProvider, MembershipType.DN, config.getMembershipLdapAttribute(), getMembershipUserLdapAttribute(), ldapParentGroup, ldapGroup);
                }
            } else {
                // No care about group inheritance. Let's just sync current group
                logger.debugf("Will sync group '%s' from DB to LDAP", groupName);
                processKeycloakGroupSyncToLDAP(kcGroup, new HashMap<>(), new HashSet<>(), new SynchronizationResult());
                ldapGroup = loadLDAPGroupByName(groupName);
            }
        }

        String membershipUserLdapAttrName = getMembershipUserLdapAttribute();

        LDAPUtils.addMember(ldapProvider, config.getMembershipTypeLdapAttribute(), config.getMembershipLdapAttribute(), membershipUserLdapAttrName, ldapGroup, ldapUser);
    }

    public void deleteGroupMappingInLDAP(LDAPObject ldapUser, LDAPObject ldapGroup) {
        String membershipUserLdapAttrName = getMembershipUserLdapAttribute();
        LDAPUtils.deleteMember(ldapProvider, config.getMembershipTypeLdapAttribute(), config.getMembershipLdapAttribute(), membershipUserLdapAttrName, ldapGroup, ldapUser);
    }

    protected List<LDAPObject> getLDAPGroupMappings(LDAPObject ldapUser) {
        String strategyKey = config.getUserGroupsRetrieveStrategy();
        UserRolesRetrieveStrategy strategy = factory.getUserGroupsRetrieveStrategy(strategyKey);

        LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();
        return strategy.getLDAPRoleMappings(this, ldapUser, ldapConfig);
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
        String strategyKey = config.getUserGroupsRetrieveStrategy();
        UserRolesRetrieveStrategy strategy = factory.getUserGroupsRetrieveStrategy(strategyKey);
        strategy.beforeUserLDAPQuery(this, query);
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        final LDAPGroupMapperMode mode = config.getMode();

        // For IMPORT mode, all operations are performed against local DB
        if (mode == LDAPGroupMapperMode.IMPORT) {
            return delegate;
        } else {
            return new LDAPGroupMappingsUserDelegate(realm, delegate, ldapUser);
        }
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        LDAPGroupMapperMode mode = config.getMode();

        // For now, import LDAP group mappings just during create
        if (mode == LDAPGroupMapperMode.IMPORT && isCreate) {

            List<LDAPObject> ldapGroups = getLDAPGroupMappings(ldapUser);
            if (!ldapGroups.isEmpty()) {
                GroupModel parent = getKcGroupsPathGroup(realm);
                // Import role mappings from LDAP into Keycloak DB
                for (LDAPObject ldapGroup : ldapGroups) {

                    GroupModel kcGroup = findKcGroupOrSyncFromLDAP(realm, parent, ldapGroup, user);
                    if (kcGroup != null) {
                        logger.debugf("User '%s' joins group '%s' during import from LDAP", user.getUsername(), kcGroup.getName());
                        user.joinGroup(kcGroup);
                    }
                }
            }
        }
    }


    protected String getMembershipUserLdapAttribute() {
        LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();
        return config.getMembershipUserLdapAttribute(ldapConfig);
    }


    public class LDAPGroupMappingsUserDelegate extends UserModelDelegate {

        private final RealmModel realm;
        private final LDAPObject ldapUser;

        // Avoid loading group mappings from LDAP more times per-request
        private Set<GroupModel> cachedLDAPGroupMappings;

        public LDAPGroupMappingsUserDelegate(RealmModel realm, UserModel user, LDAPObject ldapUser) {
            super(user);
            this.realm = realm;
            this.ldapUser = ldapUser;
        }

        @Override
        public boolean hasRole(RoleModel role) {
            return super.hasRole(role) || RoleUtils.hasRoleFromGroup(getGroupsStream(), role, true);
        }

        @Override
        public Stream<GroupModel> getGroupsStream() {
            Stream<GroupModel> ldapGroupMappings = getLDAPGroupMappingsConverted();
            if (config.isTopLevelGroupsPath() && config.getMode() == LDAPGroupMapperMode.LDAP_ONLY) {
                // Use just group mappings from LDAP
                return ldapGroupMappings;
            } else {
                // Merge mappings from both DB and LDAP (including groups assigned from other group mappers)
                return Stream.concat(ldapGroupMappings, super.getGroupsStream());
            }
        }

        @Override
        public void joinGroup(GroupModel group) {
            if (config.getMode() == LDAPGroupMapperMode.LDAP_ONLY && isGroupInGroupPath(realm, group)) {
                // We need to create new role mappings in LDAP
                cachedLDAPGroupMappings = null;
                addGroupMappingInLDAP(realm, group, ldapUser);
            } else {
                super.joinGroup(group);
            }
        }

        @Override
        public void leaveGroup(GroupModel group) {
            // if user is leaving group not managed by this mapper, let the call proceed to the next mapper or to the DB.
            if (!isGroupInGroupPath(realm, group)) {
                super.leaveGroup(group);
            }

            try (LDAPQuery ldapQuery = createGroupQuery(true)) {
                LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
                Condition roleNameCondition = conditionsBuilder.equal(config.getGroupNameLdapAttribute(), group.getName());

                String membershipUserLdapAttrName = getMembershipUserLdapAttribute();
                String membershipUserAttr = LDAPUtils.getMemberValueOfChildObject(ldapUser, config.getMembershipTypeLdapAttribute(), membershipUserLdapAttrName);
                Condition membershipCondition = conditionsBuilder.equal(config.getMembershipLdapAttribute(), membershipUserAttr);

                ldapQuery.addWhereCondition(roleNameCondition).addWhereCondition(membershipCondition);
                LDAPObject ldapGroup = ldapQuery.getFirstResult();

                if (ldapGroup == null) {
                    // Group mapping doesn't exist in LDAP. For LDAP_ONLY mode, we don't need to do anything. For READ_ONLY, delete it in local DB.
                    if (config.getMode() == LDAPGroupMapperMode.READ_ONLY) {
                        super.leaveGroup(group);
                    }
                } else {
                    // Group mapping exists in LDAP. For LDAP_ONLY mode, we can just delete it in LDAP. For READ_ONLY we can't delete it -> throw error
                    if (config.getMode() == LDAPGroupMapperMode.READ_ONLY) {
                        throw new ModelException("Not possible to delete LDAP group mappings as mapper mode is READ_ONLY");
                    } else {
                        // Delete ldap role mappings
                        cachedLDAPGroupMappings = null;
                        deleteGroupMappingInLDAP(ldapUser, ldapGroup);
                    }
                }
            }
        }

        @Override
        public boolean isMemberOf(GroupModel group) {
            if (!isGroupInGroupPath(realm, group)) {
                // this mapper doesn't manage the group - delegate to the next mapper or the JPA store.
                return super.isMemberOf(group);
            }
            return RoleUtils.isDirectMember(getGroupsStream(),group);
        }

        protected Stream<GroupModel> getLDAPGroupMappingsConverted() {
            if (cachedLDAPGroupMappings != null) {
                return cachedLDAPGroupMappings.stream();
            }

            List<LDAPObject> ldapGroups = getLDAPGroupMappings(ldapUser);
            if (!ldapGroups.isEmpty()) {
                GroupModel parent = getKcGroupsPathGroup(realm);

                cachedLDAPGroupMappings = ldapGroups.stream()
                        .map(ldapGroup -> findKcGroupOrSyncFromLDAP(realm, parent, ldapGroup, this))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                return cachedLDAPGroupMappings.stream();
            }
            return Stream.empty();
        }
    }

    // LDAP groups path operations

    /**
     * Translates given LDAP group name into a KC group within the groups path.
     */
    protected String getKcGroupPathFromLDAPGroupName(String ldapGroupName) {
        return config.getGroupsPathWithTrailingSlash() + ldapGroupName;
    }

    /**
     * Provides KC group defined as groups path or null (top-level group) if corresponding group is not available.
     */
    protected GroupModel getKcGroupsPathGroup(RealmModel realm) {
        return config.isTopLevelGroupsPath() ? null : KeycloakModelUtils.findGroupByPath(getSession(), realm, config.getGroupsPath());
    }

    protected boolean isGroupInGroupPath(RealmModel realm, GroupModel group) {
        if (group.getType() == GroupModel.Type.ORGANIZATION) {
            return false; // always skip organization groups as those are internal groups.
        }
        if (config.isTopLevelGroupsPath()) {
            return true; // any group is in the path of the top level path.
        }
        GroupModel groupPathGroup = KeycloakModelUtils.findGroupByPath(getSession(), realm, config.getGroupsPath());
        if (groupPathGroup != null) {
            while(!groupPathGroup.getId().equals(group.getId())) {
                group = group.getParent();
                if (group == null) {
                    return false; // we checked every ancestor group, and none matches the group path group.
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Creates a new KC group from given LDAP group name in given KC parent group or the groups path.
     */
    protected GroupModel createKcGroup(RealmModel realm, String ldapGroupName, GroupModel parentGroup) {

        // If no parent group given then use groups path
        if (parentGroup == null) {
            parentGroup = getKcGroupsPathGroup(realm);
        }
        return realm.createGroup(ldapGroupName, parentGroup);
    }

    /**
     * Provides a list of all KC sub groups from given parent group or from groups path.
     */
    protected Stream<GroupModel> getKcSubGroups(RealmModel realm, GroupModel parentGroup) {

        // If no parent group given then use groups path
        if (parentGroup == null) {
            parentGroup = getKcGroupsPathGroup(realm);
        }
        return parentGroup == null ? getSession().groups().getTopLevelGroupsStream(realm) :
            parentGroup.getSubGroupsStream();
    }

    /**
     * Provides a stream of all KC groups (with their sub groups) from groups path configured by the "Groups Path" configuration property.
     */
    protected Stream<GroupModel> getAllKcGroups(RealmModel realm, GroupModel topParentGroup) {
        Stream<GroupModel> allGroups = realm.getGroupsStream();
        if (topParentGroup == null) return allGroups;

        return allGroups.filter(group -> {
            // Check if group is descendant of the topParentGroup (which is group configured by "Groups Path")
            GroupModel parent = group.getParent();
            while (parent != null) {
                if (parent.getId().equals(topParentGroup.getId())) {
                    return true;
                }
                parent = parent.getParent();
            }
            return false;
        });
    }
}
