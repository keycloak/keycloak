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

package org.keycloak.storage.ldap.mappers.membership.role;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapper;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.UserRolesRetrieveStrategy;
import org.keycloak.storage.user.SynchronizationResult;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * Map realm roles or roles of particular client to LDAP groups
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleLDAPStorageMapper extends AbstractLDAPStorageMapper implements CommonLDAPGroupMapper {

    private static final Logger logger = Logger.getLogger(RoleLDAPStorageMapper.class);

    private final RoleMapperConfig config;
    private final RoleLDAPStorageMapperFactory factory;

    public RoleLDAPStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, RoleLDAPStorageMapperFactory factory) {
        super(mapperModel, ldapProvider);
        this.config = new RoleMapperConfig(mapperModel);
        this.factory = factory;
    }


    @Override
    public LDAPQuery createLDAPGroupQuery() {
        return createRoleQuery(false);
    }

    @Override
    public CommonLDAPGroupMapperConfig getConfig() {
        return config;
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        LDAPGroupMapperMode mode = config.getMode();

        // For now, import LDAP role mappings just during create
        if (mode == LDAPGroupMapperMode.IMPORT && isCreate) {

            List<LDAPObject> ldapRoles = getLDAPRoleMappings(ldapUser);

            // Import role mappings from LDAP into Keycloak DB
            String roleNameAttr = config.getRoleNameLdapAttribute();

            RoleContainerModel roleContainer = getTargetRoleContainer(realm);
            if (roleContainer == null) {
                logger.warnf("Ignored client role grant for federation mapper '%s' as client not found: '%s'", mapperModel.getName(), config.getClientId());
                return;
            }

            for (LDAPObject ldapRole : ldapRoles) {
                String roleName = ldapRole.getAttributeAsString(roleNameAttr);
                RoleModel role = roleContainer.getRole(roleName);

                if (role == null) {
                    role = roleContainer.addRole(roleName);
                }

                logger.debugf("Granting role [%s] to user [%s] during import from LDAP", roleName, user.getUsername());
                user.grantRole(role);
            }
        }
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
    }


    // Sync roles from LDAP to Keycloak DB
    @Override
    public SynchronizationResult syncDataFromFederationProviderToKeycloak(RealmModel realm) {
        SynchronizationResult syncResult = new SynchronizationResult() {

            @Override
            public String getStatus() {
                return String.format("%d imported roles, %d roles already exists in Keycloak", getAdded(), getUpdated());
            }

        };

        logger.debugf("Syncing roles from LDAP into Keycloak DB. Mapper is [%s], LDAP provider is [%s]", mapperModel.getName(), ldapProvider.getModel().getName());

        RoleContainerModel roleContainer = getTargetRoleContainer(realm);
        if (roleContainer == null) {
            logger.warnf("Ignored sync for federation mapper '%s' as client not found: '%s'", mapperModel.getName(), config.getClientId());
            return syncResult;
        }

        // Send LDAP query to load all roles
        try (LDAPQuery ldapRoleQuery = createRoleQuery(false)) {
            List<LDAPObject> ldapRoles = LDAPUtils.loadAllLDAPObjects(ldapRoleQuery, ldapProvider);

            String rolesRdnAttr = config.getRoleNameLdapAttribute();
            for (LDAPObject ldapRole : ldapRoles) {
                String roleName = ldapRole.getAttributeAsString(rolesRdnAttr);

                if (roleContainer.getRole(roleName) == null) {
                    logger.debugf("Syncing role [%s] from LDAP to keycloak DB", roleName);
                    roleContainer.addRole(roleName);
                    syncResult.increaseAdded();
                } else {
                    syncResult.increaseUpdated();
                }
            }

            return syncResult;
        }
    }


    // Sync roles from Keycloak back to LDAP
    @Override
    public SynchronizationResult syncDataFromKeycloakToFederationProvider(RealmModel realm) {
        SynchronizationResult syncResult = new SynchronizationResult() {

            @Override
            public String getStatus() {
                return String.format("%d roles imported to LDAP, %d roles already existed in LDAP", getAdded(), getUpdated());
            }

        };

        if (config.getMode() != LDAPGroupMapperMode.LDAP_ONLY) {
            logger.warnf("Ignored sync for federation mapper '%s' as it's mode is '%s'", mapperModel.getName(), config.getMode().toString());
            return syncResult;
        }

        logger.debugf("Syncing roles from Keycloak into LDAP. Mapper is [%s], LDAP provider is [%s]", mapperModel.getName(), ldapProvider.getModel().getName());

        RoleContainerModel roleContainer = getTargetRoleContainer(realm);
        if (roleContainer == null) {
            logger.warnf("Ignored sync for federation mapper '%s' as client not found: '%s'", mapperModel.getName(), config.getClientId());
            return syncResult;
        }

        // Send LDAP query to see which roles exists there
        try (LDAPQuery ldapQuery = createRoleQuery(false)) {
            List<LDAPObject> ldapRoles = LDAPUtils.loadAllLDAPObjects(ldapQuery, ldapProvider);

            Set<String> ldapRoleNames = new HashSet<>();
            String rolesRdnAttr = config.getRoleNameLdapAttribute();
            for (LDAPObject ldapRole : ldapRoles) {
                String roleName = ldapRole.getAttributeAsString(rolesRdnAttr);
                ldapRoleNames.add(roleName);
            }


            Stream<RoleModel> keycloakRoles = roleContainer.getRolesStream();

            Consumer<String> syncRoleFromKCToLDAP = roleName -> {
                if (ldapRoleNames.contains(roleName)) {
                    syncResult.increaseUpdated();
                } else {
                    logger.debugf("Syncing role [%s] from Keycloak to LDAP", roleName);
                    createLDAPRole(roleName);
                    syncResult.increaseAdded();
                }
            };
            keycloakRoles.map(RoleModel::getName).forEach(syncRoleFromKCToLDAP);

            return syncResult;
        }
    }

    // TODO: Possible to merge with GroupMapper and move to common class
    public LDAPQuery createRoleQuery(boolean includeMemberAttribute) {
        LDAPQuery ldapQuery = new LDAPQuery(ldapProvider);

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapQuery.setSearchScope(ldapProvider.getLdapIdentityStore().getConfig().getSearchScope());

        String rolesDn = config.getRolesDn();
        ldapQuery.setSearchDn(rolesDn);

        Collection<String> roleObjectClasses = config.getRoleObjectClasses(ldapProvider);
        ldapQuery.addObjectClasses(roleObjectClasses);

        String rolesRdnAttr = config.getRoleNameLdapAttribute();

        String customFilter = config.getCustomLdapFilter();
        if (customFilter != null && customFilter.trim().length() > 0) {
            Condition customFilterCondition = new LDAPQueryConditionsBuilder().addCustomLDAPFilter(customFilter);
            ldapQuery.addWhereCondition(customFilterCondition);
        }

        ldapQuery.addReturningLdapAttribute(rolesRdnAttr);

        // Performance improvement
        if (includeMemberAttribute) {
            String membershipAttr = config.getMembershipLdapAttribute();
            ldapQuery.addReturningLdapAttribute(membershipAttr);
        }

        return ldapQuery;
    }

    protected RoleContainerModel getTargetRoleContainer(RealmModel realm) {
        boolean realmRolesMapping = config.isRealmRolesMapping();
        if (realmRolesMapping) {
            return realm;
        } else {
            String clientId = config.getClientId();
            if (clientId == null) {
                throw new ModelException("Using client roles mapping is requested, but parameter client.id not found!");
            }
            ClientModel client = realm.getClientByClientId(clientId);
            if (client == null) {
                logger.warnf("Cannot find requested client with clientId '%s' in federation mapper '%s'", clientId, mapperModel.getName());
            }
            return client;
        }
    }


    public LDAPObject createLDAPRole(String roleName) {
        LDAPObject ldapRole = LDAPUtils.createLDAPGroup(ldapProvider, roleName, config.getRoleNameLdapAttribute(), config.getRoleObjectClasses(ldapProvider),
                config.getRelativeCreateDn() + config.getRolesDn(), Collections.<String, Set<String>>emptyMap(), config.getMembershipLdapAttribute());

        logger.debugf("Creating role [%s] to LDAP with DN [%s]", roleName, ldapRole.getDn().toString());
        return ldapRole;
    }

    public void addRoleMappingInLDAP(String roleName, LDAPObject ldapUser) {
        LDAPObject ldapRole = loadLDAPRoleByName(roleName);
        if (ldapRole == null) {
            ldapRole = createLDAPRole(roleName);
        }

        String membershipUserAttrName = getMembershipUserLdapAttribute();

        LDAPUtils.addMember(ldapProvider, config.getMembershipTypeLdapAttribute(), config.getMembershipLdapAttribute(), membershipUserAttrName, ldapRole, ldapUser);
    }

    public void deleteRoleMappingInLDAP(LDAPObject ldapUser, LDAPObject ldapRole) {
        String membershipUserAttrName = getMembershipUserLdapAttribute();
        LDAPUtils.deleteMember(ldapProvider, config.getMembershipTypeLdapAttribute(), config.getMembershipLdapAttribute(), membershipUserAttrName, ldapRole, ldapUser);
    }

    public LDAPObject loadLDAPRoleByName(String roleName) {
        try (LDAPQuery ldapQuery = createRoleQuery(true)) {
            Condition roleNameCondition = new LDAPQueryConditionsBuilder().equal(config.getRoleNameLdapAttribute(), roleName);
            ldapQuery.addWhereCondition(roleNameCondition);
            return ldapQuery.getFirstResult();
        }
    }

    protected List<LDAPObject> getLDAPRoleMappings(LDAPObject ldapUser) {
        String strategyKey = config.getUserRolesRetrieveStrategy();
        UserRolesRetrieveStrategy strategy = factory.getUserRolesRetrieveStrategy(strategyKey);

        LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();
        return strategy.getLDAPRoleMappings(this, ldapUser, ldapConfig);
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        final LDAPGroupMapperMode mode = config.getMode();

        // For IMPORT mode, all operations are performed against local DB
        if (mode == LDAPGroupMapperMode.IMPORT) {
            return delegate;
        }
        final RoleContainerModel targetRoleContainer = getTargetRoleContainer(realm);
        if (targetRoleContainer == null) {
            return delegate;
        } else {
            return new LDAPRoleMappingsUserDelegate(realm, delegate, ldapUser, targetRoleContainer);
        }
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
        String strategyKey = config.getUserRolesRetrieveStrategy();
        UserRolesRetrieveStrategy strategy = factory.getUserRolesRetrieveStrategy(strategyKey);
        strategy.beforeUserLDAPQuery(this, query);
    }


    protected String getMembershipUserLdapAttribute() {
        LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();
        return config.getMembershipUserLdapAttribute(ldapConfig);
    }


    public class LDAPRoleMappingsUserDelegate extends UserModelDelegate {

        private final RealmModel realm;
        private final LDAPObject ldapUser;
        private final RoleContainerModel roleContainer;

        // Avoid loading role mappings from LDAP more times per-request
        private Set<RoleModel> cachedLDAPRoleMappings;

        public LDAPRoleMappingsUserDelegate(RealmModel realm, UserModel user, LDAPObject ldapUser, RoleContainerModel targetRoleContainer) {
            super(user);
            this.realm = realm;
            this.ldapUser = ldapUser;
            this.roleContainer = targetRoleContainer;
        }

        @Override
        public Stream<RoleModel> getRealmRoleMappingsStream() {
            if (roleContainer.equals(realm)) {
                Stream<RoleModel> ldapRoleMappings = getLDAPRoleMappingsConverted();

                if (config.getMode() == LDAPGroupMapperMode.LDAP_ONLY) {
                    // Use just role mappings from LDAP
                    return ldapRoleMappings;
                } else {
                    // Merge mappings from both DB and LDAP
                    return Stream.concat(ldapRoleMappings, super.getRealmRoleMappingsStream());
                }
            } else {
                return super.getRealmRoleMappingsStream();
            }
        }

        @Override
        public Stream<RoleModel> getClientRoleMappingsStream(ClientModel client) {
            if (roleContainer.equals(client)) {
                Stream<RoleModel> ldapRoleMappings = getLDAPRoleMappingsConverted();

                if (config.getMode() == LDAPGroupMapperMode.LDAP_ONLY) {
                    // Use just role mappings from LDAP
                    return ldapRoleMappings;
                } else {
                    // Merge mappings from both DB and LDAP
                    return Stream.concat(ldapRoleMappings, super.getClientRoleMappingsStream(client));
                }
            } else {
                return super.getClientRoleMappingsStream(client);
            }
        }

        @Override
        public boolean hasRole(RoleModel role) {
            return RoleUtils.hasRole(getRoleMappingsStream(), role)
              || RoleUtils.hasRoleFromGroup(getGroupsStream(), role, true);
        }

        @Override
        public void grantRole(RoleModel role) {
            if (config.getMode() == LDAPGroupMapperMode.LDAP_ONLY) {

                if (role.getContainer().equals(roleContainer)) {

                    // We need to create new role mappings in LDAP
                    cachedLDAPRoleMappings = null;
                    addRoleMappingInLDAP(role.getName(), ldapUser);
                } else {
                    super.grantRole(role);
                }
            } else {
                super.grantRole(role);
            }
        }

        @Override
        public Stream<RoleModel> getRoleMappingsStream() {
            Stream<RoleModel> modelRoleMappings = super.getRoleMappingsStream();

            Stream<RoleModel> ldapRoleMappings = getLDAPRoleMappingsConverted();

            if (config.getMode() == LDAPGroupMapperMode.LDAP_ONLY) {
                // For LDAP-only we want to retrieve role mappings of target container just from LDAP
                modelRoleMappings = modelRoleMappings.filter(role -> !Objects.equals(role.getContainer(), roleContainer));
            }

            return Stream.concat(modelRoleMappings, ldapRoleMappings);
        }

        protected Stream<RoleModel> getLDAPRoleMappingsConverted() {
            if (cachedLDAPRoleMappings != null) {
                return cachedLDAPRoleMappings.stream();
            }

            List<LDAPObject> ldapRoles = getLDAPRoleMappings(ldapUser);
            String roleNameLdapAttr = config.getRoleNameLdapAttribute();
            cachedLDAPRoleMappings = ldapRoles.stream()
                    .map(role -> {
                        String roleName = role.getAttributeAsString(roleNameLdapAttr);
                        RoleModel modelRole = roleContainer.getRole(roleName);
                        if (modelRole == null) {
                            // Add role to local DB
                            modelRole = roleContainer.addRole(roleName);
                        }
                        return modelRole;
                    }).collect(Collectors.toSet());

            return cachedLDAPRoleMappings.stream();
        }

        @Override
        public void deleteRoleMapping(RoleModel role) {
            if (role.getContainer().equals(roleContainer)) {

                try (LDAPQuery ldapQuery = createRoleQuery(true)) {
                    LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
                    Condition roleNameCondition = conditionsBuilder.equal(config.getRoleNameLdapAttribute(), role.getName());

                    String membershipUserAttrName = getMembershipUserLdapAttribute();
                    String membershipUserAttr = LDAPUtils.getMemberValueOfChildObject(ldapUser, config.getMembershipTypeLdapAttribute(), membershipUserAttrName);

                    Condition membershipCondition = conditionsBuilder.equal(config.getMembershipLdapAttribute(), membershipUserAttr);

                    ldapQuery.addWhereCondition(roleNameCondition).addWhereCondition(membershipCondition);
                    LDAPObject ldapRole = ldapQuery.getFirstResult();

                    if (ldapRole == null) {
                        // Role mapping doesn't exist in LDAP. For LDAP_ONLY mode, we don't need to do anything. For READ_ONLY, delete it in local DB.
                        if (config.getMode() == LDAPGroupMapperMode.READ_ONLY) {
                            super.deleteRoleMapping(role);
                        }
                    } else {
                        // Role mappings exists in LDAP. For LDAP_ONLY mode, we can just delete it in LDAP. For READ_ONLY we can't delete it -> throw error
                        if (config.getMode() == LDAPGroupMapperMode.READ_ONLY) {
                            throw new ModelException("Not possible to delete LDAP role mappings as mapper mode is READ_ONLY");
                        } else {
                            // Delete ldap role mappings
                            cachedLDAPRoleMappings = null;
                            deleteRoleMappingInLDAP(ldapUser, ldapRole);
                        }
                    }
                }
            } else {
                super.deleteRoleMapping(role);
            }
        }
    }

    public LDAPObject loadRoleGroupByName(String roleName) {
        try (LDAPQuery ldapQuery = createRoleQuery(true)) {
            Condition roleNameCondition = new LDAPQueryConditionsBuilder().equal(config.getRoleNameLdapAttribute(), roleName);
            ldapQuery.addWhereCondition(roleNameCondition);
            return ldapQuery.getFirstResult();
        }
    }

    @Override
    public List<UserModel> getRoleMembers(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        if (config.getMode() == LDAPGroupMapperMode.IMPORT) {
            // only results from Keycloak should be returned, or imported LDAP and KC items will duplicate
            return Collections.emptyList();
        }

        LDAPObject ldapGroup = loadRoleGroupByName(role.getName());
        if (ldapGroup == null) {
            return Collections.emptyList();
        }

        String strategyKey = config.getUserRolesRetrieveStrategy();
        UserRolesRetrieveStrategy strategy = factory.getUserRolesRetrieveStrategy(strategyKey);
        return strategy.getLDAPRoleMembers(realm, this, ldapGroup, firstResult, maxResults);
    }
}
