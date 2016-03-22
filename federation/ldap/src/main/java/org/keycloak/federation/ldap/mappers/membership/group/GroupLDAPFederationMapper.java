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

package org.keycloak.federation.ldap.mappers.membership.group;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.federation.ldap.mappers.AbstractLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.membership.CommonLDAPGroupMapper;
import org.keycloak.federation.ldap.mappers.membership.CommonLDAPGroupMapperConfig;
import org.keycloak.federation.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.federation.ldap.mappers.membership.MembershipType;
import org.keycloak.federation.ldap.mappers.membership.UserRolesRetrieveStrategy;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupLDAPFederationMapper extends AbstractLDAPFederationMapper implements CommonLDAPGroupMapper {

    private static final Logger logger = Logger.getLogger(GroupLDAPFederationMapper.class);

    private final GroupMapperConfig config;
    private final GroupLDAPFederationMapperFactory factory;

    // Flag to avoid syncing multiple times per transaction
    private boolean syncFromLDAPPerformedInThisTransaction = false;

    public GroupLDAPFederationMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, RealmModel realm, GroupLDAPFederationMapperFactory factory) {
        super(mapperModel, ldapProvider, realm);
        this.config = new GroupMapperConfig(mapperModel);
        this.factory = factory;
    }


    // CommonLDAPGroupMapper interface

    @Override
    public LDAPQuery createLDAPGroupQuery() {
        return createGroupQuery();
    }

    @Override
    public CommonLDAPGroupMapperConfig getConfig() {
        return config;
    }



    // LDAP Group CRUD operations

    public LDAPQuery createGroupQuery() {
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
        ldapQuery.addReturningLdapAttribute(config.getMembershipLdapAttribute());

        for (String groupAttr : config.getGroupAttributes()) {
            ldapQuery.addReturningLdapAttribute(groupAttr);
        }

        return ldapQuery;
    }

    public LDAPObject createLDAPGroup(String groupName, Map<String, Set<String>> additionalAttributes) {
        LDAPObject ldapGroup = LDAPUtils.createLDAPGroup(ldapProvider, groupName, config.getGroupNameLdapAttribute(), config.getGroupObjectClasses(ldapProvider),
                config.getGroupsDn(), additionalAttributes);

        logger.debugf("Creating group [%s] to LDAP with DN [%s]", groupName, ldapGroup.getDn().toString());
        return ldapGroup;
    }

    public LDAPObject loadLDAPGroupByName(String groupName) {
        LDAPQuery ldapQuery = createGroupQuery();
        Condition roleNameCondition = new LDAPQueryConditionsBuilder().equal(config.getGroupNameLdapAttribute(), groupName);
        ldapQuery.addWhereCondition(roleNameCondition);
        return ldapQuery.getFirstResult();
    }

    protected Set<LDAPDn> getLDAPSubgroups(LDAPObject ldapGroup) {
        MembershipType membershipType = config.getMembershipTypeLdapAttribute();
        return membershipType.getLDAPSubgroups(this, ldapGroup);
    }


    // Sync from Ldap to KC

    public UserFederationSyncResult syncDataFromFederationProviderToKeycloak() {
        UserFederationSyncResult syncResult = new UserFederationSyncResult() {

            @Override
            public String getStatus() {
                return String.format("%d imported groups, %d updated groups, %d removed groups", getAdded(), getUpdated(), getRemoved());
            }

        };

        logger.debugf("Syncing groups from LDAP into Keycloak DB. Mapper is [%s], LDAP provider is [%s]", mapperModel.getName(), ldapProvider.getModel().getDisplayName());

        // Get all LDAP groups
        List<LDAPObject> ldapGroups = getAllLDAPGroups();

        // Convert to internal format
        Map<String, LDAPObject> ldapGroupsMap = new HashMap<>();
        List<GroupTreeResolver.Group> ldapGroupsRep = new LinkedList<>();

        String groupsRdnAttr = config.getGroupNameLdapAttribute();
        for (LDAPObject ldapGroup : ldapGroups) {
            String groupName = ldapGroup.getAttributeAsString(groupsRdnAttr);

            Set<String> subgroupNames = new HashSet<>();
            for (LDAPDn groupDn : getLDAPSubgroups(ldapGroup)) {
                subgroupNames.add(groupDn.getFirstRdnAttrValue());
            }

            ldapGroupsRep.add(new GroupTreeResolver.Group(groupName, subgroupNames));
            ldapGroupsMap.put(groupName, ldapGroup);
        }

        // Now we have list of LDAP groups. Let's form the tree (if needed)
        if (config.isPreserveGroupsInheritance()) {
            try {
                List<GroupTreeResolver.GroupTreeEntry> groupTrees = new GroupTreeResolver().resolveGroupTree(ldapGroupsRep);

                updateKeycloakGroupTree(groupTrees, ldapGroupsMap, syncResult);
            } catch (GroupTreeResolver.GroupTreeResolveException gre) {
                throw new ModelException("Couldn't resolve groups from LDAP. Fix LDAP or skip preserve inheritance. Details: " + gre.getMessage(), gre);
            }
        } else {
            Set<String> visitedGroupIds = new HashSet<>();

            // Just add flat structure of groups with all groups at top-level
            for (Map.Entry<String, LDAPObject> groupEntry : ldapGroupsMap.entrySet()) {
                String groupName = groupEntry.getKey();
                GroupModel kcExistingGroup = KeycloakModelUtils.findGroupByPath(realm, "/" + groupName);

                if (kcExistingGroup != null) {
                    updateAttributesOfKCGroup(kcExistingGroup, groupEntry.getValue());
                    syncResult.increaseUpdated();
                    visitedGroupIds.add(kcExistingGroup.getId());
                } else {
                    GroupModel kcGroup = realm.createGroup(groupName);
                    updateAttributesOfKCGroup(kcGroup, groupEntry.getValue());
                    realm.moveGroup(kcGroup, null);
                    syncResult.increaseAdded();
                    visitedGroupIds.add(kcGroup.getId());
                }
            }

            // Possibly remove keycloak groups, which doesn't exists in LDAP
            if (config.isDropNonExistingGroupsDuringSync()) {
                dropNonExistingKcGroups(syncResult, visitedGroupIds);
            }
        }

        syncFromLDAPPerformedInThisTransaction = true;

        return syncResult;
    }

    private void updateKeycloakGroupTree(List<GroupTreeResolver.GroupTreeEntry> groupTrees, Map<String, LDAPObject> ldapGroups, UserFederationSyncResult syncResult) {
        Set<String> visitedGroupIds = new HashSet<>();

        for (GroupTreeResolver.GroupTreeEntry groupEntry : groupTrees) {
            updateKeycloakGroupTreeEntry(groupEntry, ldapGroups, null, syncResult, visitedGroupIds);
        }

        // Possibly remove keycloak groups, which doesn't exists in LDAP
        if (config.isDropNonExistingGroupsDuringSync()) {
            dropNonExistingKcGroups(syncResult, visitedGroupIds);
        }
    }

    private void updateKeycloakGroupTreeEntry(GroupTreeResolver.GroupTreeEntry groupTreeEntry, Map<String, LDAPObject> ldapGroups, GroupModel kcParent, UserFederationSyncResult syncResult, Set<String> visitedGroupIds) {
        String groupName = groupTreeEntry.getGroupName();

        // Check if group already exists
        GroupModel kcGroup = null;
        Collection<GroupModel> subgroups = kcParent == null ? realm.getTopLevelGroups() : kcParent.getSubGroups();
        for (GroupModel group : subgroups) {
            if (group.getName().equals(groupName)) {
                kcGroup = group;
                break;
            }
        }

        if (kcGroup != null) {
            logger.debugf("Updated Keycloak group '%s' from LDAP", kcGroup.getName());
            updateAttributesOfKCGroup(kcGroup, ldapGroups.get(kcGroup.getName()));
            syncResult.increaseUpdated();
        } else {
            kcGroup = realm.createGroup(groupTreeEntry.getGroupName());
            if (kcParent == null) {
                realm.moveGroup(kcGroup, null);
                logger.debugf("Imported top-level group '%s' from LDAP", kcGroup.getName());
            } else {
                realm.moveGroup(kcGroup, kcParent);
                logger.debugf("Imported group '%s' from LDAP as child of group '%s'", kcGroup.getName(), kcParent.getName());
            }

            updateAttributesOfKCGroup(kcGroup, ldapGroups.get(kcGroup.getName()));
            syncResult.increaseAdded();
        }

        visitedGroupIds.add(kcGroup.getId());

        for (GroupTreeResolver.GroupTreeEntry childEntry : groupTreeEntry.getChildren()) {
            updateKeycloakGroupTreeEntry(childEntry, ldapGroups, kcGroup, syncResult, visitedGroupIds);
        }
    }

    private void dropNonExistingKcGroups(UserFederationSyncResult syncResult, Set<String> visitedGroupIds) {
        // Remove keycloak groups, which doesn't exists in LDAP
        List<GroupModel> allGroups = realm.getGroups();
        for (GroupModel kcGroup : allGroups) {
            if (!visitedGroupIds.contains(kcGroup.getId())) {
                logger.debugf("Removing Keycloak group '%s', which doesn't exist in LDAP", kcGroup.getName());
                realm.removeGroup(kcGroup);
                syncResult.increaseRemoved();
            }
        }
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


    protected GroupModel findKcGroupByLDAPGroup(LDAPObject ldapGroup) {
        String groupNameAttr = config.getGroupNameLdapAttribute();
        String groupName = ldapGroup.getAttributeAsString(groupNameAttr);

        if (config.isPreserveGroupsInheritance()) {
            // Override if better effectivity or different algorithm is needed
            List<GroupModel> groups = realm.getGroups();
            for (GroupModel group : groups) {
                if (group.getName().equals(groupName)) {
                    return group;
                }
            }

            return null;
        } else {
            // Without preserved inheritance, it's always top-level group
            return KeycloakModelUtils.findGroupByPath(realm, "/" + groupName);
        }
    }

    protected GroupModel findKcGroupOrSyncFromLDAP(LDAPObject ldapGroup, UserModel user) {
        GroupModel kcGroup = findKcGroupByLDAPGroup(ldapGroup);

        if (kcGroup == null) {

            if (config.isPreserveGroupsInheritance()) {

                // Better to sync all groups from LDAP with preserved inheritance
                if (!syncFromLDAPPerformedInThisTransaction) {
                    syncDataFromFederationProviderToKeycloak();
                    kcGroup = findKcGroupByLDAPGroup(ldapGroup);
                }
            } else {
                String groupNameAttr = config.getGroupNameLdapAttribute();
                String groupName = ldapGroup.getAttributeAsString(groupNameAttr);

                kcGroup = realm.createGroup(groupName);
                updateAttributesOfKCGroup(kcGroup, ldapGroup);
                realm.moveGroup(kcGroup, null);
            }

            // Could theoretically happen on some LDAP servers if 'memberof' style is used and 'memberof' attribute of user references non-existing group
            if (kcGroup == null) {
                String groupName = ldapGroup.getAttributeAsString(config.getGroupNameLdapAttribute());
                logger.warnf("User '%s' is member of group '%s', which doesn't exists in LDAP", user.getUsername(), groupName);
            }
        }

        return kcGroup;
    }

    // Send LDAP query to retrieve all groups
    protected List<LDAPObject> getAllLDAPGroups() {
        LDAPQuery ldapGroupQuery = createGroupQuery();
        return LDAPUtils.loadAllLDAPObjects(ldapGroupQuery, ldapProvider);
    }


    // Sync from Keycloak to LDAP

    public UserFederationSyncResult syncDataFromKeycloakToFederationProvider() {
        UserFederationSyncResult syncResult = new UserFederationSyncResult() {

            @Override
            public String getStatus() {
                return String.format("%d groups imported to LDAP, %d groups updated to LDAP, %d groups removed from LDAP", getAdded(), getUpdated(), getRemoved());
            }

        };

        if (config.getMode() != LDAPGroupMapperMode.LDAP_ONLY) {
            logger.warnf("Ignored sync for federation mapper '%s' as it's mode is '%s'", mapperModel.getName(), config.getMode().toString());
            return syncResult;
        }

        logger.debugf("Syncing groups from Keycloak into LDAP. Mapper is [%s], LDAP provider is [%s]", mapperModel.getName(), ldapProvider.getModel().getDisplayName());

        // Query existing LDAP groups
        LDAPQuery ldapQuery = createGroupQuery();
        List<LDAPObject> ldapGroups = ldapQuery.getResultList();

        // Convert them to Map<String, LDAPObject>
        Map<String, LDAPObject> ldapGroupsMap = new HashMap<>();
        String groupsRdnAttr = config.getGroupNameLdapAttribute();
        for (LDAPObject ldapGroup : ldapGroups) {
            String groupName = ldapGroup.getAttributeAsString(groupsRdnAttr);
            ldapGroupsMap.put(groupName, ldapGroup);
        }

        // Map to track all LDAP groups also exists in Keycloak
        Set<String> ldapGroupNames = new HashSet<>();

        // Create or update KC groups to LDAP including their attributes
        for (GroupModel kcGroup : realm.getTopLevelGroups()) {
            processLdapGroupSyncToLDAP(kcGroup, ldapGroupsMap, ldapGroupNames, syncResult);
        }

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
            for (GroupModel kcGroup : realm.getTopLevelGroups()) {
                processLdapGroupMembershipsSyncToLDAP(kcGroup, ldapGroupsMap);
            }
        }

        return syncResult;
    }

    // For given kcGroup check if it exists in LDAP (map) by name
    // If not, create it in LDAP including attributes. Otherwise update attributes in LDAP.
    // Process this recursively for all subgroups of KC group
    private void processLdapGroupSyncToLDAP(GroupModel kcGroup, Map<String, LDAPObject> ldapGroupsMap, Set<String> ldapGroupNames, UserFederationSyncResult syncResult) {
        String groupName = kcGroup.getName();

        // extract group attributes to be updated to LDAP
        Map<String, Set<String>> supportedLdapAttributes = new HashMap<>();
        for (String attrName : config.getGroupAttributes()) {
            List<String> kcAttrValues = kcGroup.getAttribute(attrName);
            Set<String> attrValues2 = (kcAttrValues == null || kcAttrValues.isEmpty()) ? null : new HashSet<>(kcAttrValues);
            supportedLdapAttributes.put(attrName, attrValues2);
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
        for (GroupModel kcSubgroup : kcGroup.getSubGroups()) {
            processLdapGroupSyncToLDAP(kcSubgroup, ldapGroupsMap, ldapGroupNames, syncResult);
        }
    }

    // Sync memberships update. Update memberships of group in LDAP based on subgroups from KC. Do it recursively
    private void processLdapGroupMembershipsSyncToLDAP(GroupModel kcGroup, Map<String, LDAPObject> ldapGroupsMap) {
        LDAPObject ldapGroup = ldapGroupsMap.get(kcGroup.getName());
        Set<LDAPDn> toRemoveSubgroupsDNs = getLDAPSubgroups(ldapGroup);

        // Add LDAP subgroups, which are KC subgroups
        Set<GroupModel> kcSubgroups = kcGroup.getSubGroups();
        for (GroupModel kcSubgroup : kcSubgroups) {
            LDAPObject ldapSubgroup = ldapGroupsMap.get(kcSubgroup.getName());
            LDAPUtils.addMember(ldapProvider, MembershipType.DN, config.getMembershipLdapAttribute(), ldapGroup, ldapSubgroup, false);
            toRemoveSubgroupsDNs.remove(ldapSubgroup.getDn());
        }

        // Remove LDAP subgroups, which are not members in KC anymore
        for (LDAPDn toRemoveDN : toRemoveSubgroupsDNs) {
            LDAPObject fakeGroup = new LDAPObject();
            fakeGroup.setDn(toRemoveDN);
            LDAPUtils.deleteMember(ldapProvider, MembershipType.DN, config.getMembershipLdapAttribute(), ldapGroup, fakeGroup, false);
        }

        // Update group to LDAP
        if (!kcGroup.getSubGroups().isEmpty() || !toRemoveSubgroupsDNs.isEmpty()) {
            ldapProvider.getLdapIdentityStore().update(ldapGroup);
        }

        for (GroupModel kcSubgroup : kcGroup.getSubGroups()) {
            processLdapGroupMembershipsSyncToLDAP(kcSubgroup, ldapGroupsMap);
        }
    }


    // group-user membership operations


    @Override
    public List<UserModel> getGroupMembers(GroupModel kcGroup, int firstResult, int maxResults) {
        LDAPObject ldapGroup = loadLDAPGroupByName(kcGroup.getName());
        if (ldapGroup == null) {
            return Collections.emptyList();
        }

        MembershipType membershipType = config.getMembershipTypeLdapAttribute();
        return membershipType.getGroupMembers(this, ldapGroup, firstResult, maxResults);
    }

    public void addGroupMappingInLDAP(String groupName, LDAPObject ldapUser) {
        LDAPObject ldapGroup = loadLDAPGroupByName(groupName);
        if (ldapGroup == null) {
            syncDataFromKeycloakToFederationProvider();
            ldapGroup = loadLDAPGroupByName(groupName);
        }

        LDAPUtils.addMember(ldapProvider, config.getMembershipTypeLdapAttribute(), config.getMembershipLdapAttribute(), ldapGroup, ldapUser, true);
    }

    public void deleteGroupMappingInLDAP(LDAPObject ldapUser, LDAPObject ldapGroup) {
        LDAPUtils.deleteMember(ldapProvider, config.getMembershipTypeLdapAttribute(), config.getMembershipLdapAttribute(), ldapGroup, ldapUser, true);
    }

    protected List<LDAPObject> getLDAPGroupMappings(LDAPObject ldapUser) {
        String strategyKey = config.getUserGroupsRetrieveStrategy();
        UserRolesRetrieveStrategy strategy = factory.getUserGroupsRetrieveStrategy(strategyKey);
        return strategy.getLDAPRoleMappings(this, ldapUser);
    }

    public void beforeLDAPQuery(LDAPQuery query) {
        String strategyKey = config.getUserGroupsRetrieveStrategy();
        UserRolesRetrieveStrategy strategy = factory.getUserGroupsRetrieveStrategy(strategyKey);
        strategy.beforeUserLDAPQuery(query);
    }

    public UserModel proxy(LDAPObject ldapUser, UserModel delegate) {
        final LDAPGroupMapperMode mode = config.getMode();

        // For IMPORT mode, all operations are performed against local DB
        if (mode == LDAPGroupMapperMode.IMPORT) {
            return delegate;
        } else {
            return new LDAPGroupMappingsUserDelegate(delegate, ldapUser);
        }
    }

    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser) {
    }

    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, boolean isCreate) {
        LDAPGroupMapperMode mode = config.getMode();

        // For now, import LDAP group mappings just during create
        if (mode == LDAPGroupMapperMode.IMPORT && isCreate) {

            List<LDAPObject> ldapGroups = getLDAPGroupMappings(ldapUser);

            // Import role mappings from LDAP into Keycloak DB
            for (LDAPObject ldapGroup : ldapGroups) {

                GroupModel kcGroup = findKcGroupOrSyncFromLDAP(ldapGroup, user);
                if (kcGroup != null) {
                    logger.debugf("User '%s' joins group '%s' during import from LDAP", user.getUsername(), kcGroup.getName());
                    user.joinGroup(kcGroup);
                }
            }
        }
    }


    public class LDAPGroupMappingsUserDelegate extends UserModelDelegate {

        private final LDAPObject ldapUser;

        // Avoid loading group mappings from LDAP more times per-request
        private Set<GroupModel> cachedLDAPGroupMappings;

        public LDAPGroupMappingsUserDelegate(UserModel user, LDAPObject ldapUser) {
            super(user);
            this.ldapUser = ldapUser;
        }

        @Override
        public Set<GroupModel> getGroups() {
            Set<GroupModel> ldapGroupMappings = getLDAPGroupMappingsConverted();
            if (config.getMode() == LDAPGroupMapperMode.LDAP_ONLY) {
                // Use just group mappings from LDAP
                return ldapGroupMappings;
            } else {
                // Merge mappings from both DB and LDAP
                Set<GroupModel> modelGroupMappings = super.getGroups();
                ldapGroupMappings.addAll(modelGroupMappings);
                return ldapGroupMappings;
            }
        }

        @Override
        public void joinGroup(GroupModel group) {
            if (config.getMode() == LDAPGroupMapperMode.LDAP_ONLY) {
                // We need to create new role mappings in LDAP
                cachedLDAPGroupMappings = null;
                addGroupMappingInLDAP(group.getName(), ldapUser);
            } else {
                super.joinGroup(group);
            }
        }

        @Override
        public void leaveGroup(GroupModel group) {
            LDAPQuery ldapQuery = createGroupQuery();
            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
            Condition roleNameCondition = conditionsBuilder.equal(config.getGroupNameLdapAttribute(), group.getName());
            String membershipUserAttr = LDAPUtils.getMemberValueOfChildObject(ldapUser, config.getMembershipTypeLdapAttribute());
            Condition membershipCondition = conditionsBuilder.equal(config.getMembershipLdapAttribute(), membershipUserAttr);
            ldapQuery.addWhereCondition(roleNameCondition).addWhereCondition(membershipCondition);
            LDAPObject ldapGroup = ldapQuery.getFirstResult();

            if (ldapGroup == null) {
                // Group mapping doesn't exist in LDAP. For LDAP_ONLY mode, we don't need to do anything. For READ_ONLY, delete it in local DB.
                if (config.getMode() == LDAPGroupMapperMode.READ_ONLY) {
                    super.leaveGroup(group);
                }
            } else {
                // Group mappings exists in LDAP. For LDAP_ONLY mode, we can just delete it in LDAP. For READ_ONLY we can't delete it -> throw error
                if (config.getMode() == LDAPGroupMapperMode.READ_ONLY) {
                    throw new ModelException("Not possible to delete LDAP group mappings as mapper mode is READ_ONLY");
                } else {
                    // Delete ldap role mappings
                    cachedLDAPGroupMappings = null;
                    deleteGroupMappingInLDAP(ldapUser, ldapGroup);
                }
            }
        }

        @Override
        public boolean isMemberOf(GroupModel group) {
            Set<GroupModel> ldapGroupMappings = getGroups();
            return ldapGroupMappings.contains(group);
        }

        protected Set<GroupModel> getLDAPGroupMappingsConverted() {
            if (cachedLDAPGroupMappings != null) {
                return new HashSet<>(cachedLDAPGroupMappings);
            }

            List<LDAPObject> ldapGroups = getLDAPGroupMappings(ldapUser);

            Set<GroupModel> result = new HashSet<>();
            for (LDAPObject ldapGroup : ldapGroups) {
                GroupModel kcGroup = findKcGroupOrSyncFromLDAP(ldapGroup, this);
                if (kcGroup != null) {
                    result.add(kcGroup);
                }
            }

            cachedLDAPGroupMappings = new HashSet<>(result);

            return result;
        }
    }
}
