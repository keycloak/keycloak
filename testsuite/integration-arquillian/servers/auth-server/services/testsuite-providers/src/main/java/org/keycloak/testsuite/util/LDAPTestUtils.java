/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.util;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPTestUtils {

    public static UserModel addLocalUser(KeycloakSession session, RealmModel realm, String username, String email, String password) {
        UserModel user = session.userLocalStorage().addUser(realm, username);
        user.setEmail(email);
        user.setEnabled(true);

        UserCredentialModel creds = UserCredentialModel.password(password);

        session.userCredentialManager().updateCredential(realm, user, creds);
        return user;
    }

    public static void addLdapUser(KeycloakSession session, RealmModel appRealm, LDAPStorageProvider ldapFedProvider, String username, String password, Consumer<UserModel> userCustomizer) {

        UserModel user = ldapFedProvider.addUser(appRealm, username);

        userCustomizer.accept(user);

        if (password == null) {
            return;
        }
        session.userCredentialManager().updateCredential(appRealm, user, (UserCredentialModel) UserCredentialModel.password(username));
    }

    public static LDAPObject addLDAPUser(LDAPStorageProvider ldapProvider, RealmModel realm, final String username,
                                         final String firstName, final String lastName, final String email, final String street, final String... postalCode) {
        UserModel helperUser = new UserModelDelegate(null) {

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public String getFirstName() {
                return firstName;
            }

            @Override
            public String getLastName() {
                return lastName;
            }

            @Override
            public String getFirstAttribute(String name) {
                if (UserModel.LAST_NAME.equals(name)) {
                    return lastName;
                } else if (UserModel.FIRST_NAME.equals(name)) {
                    return firstName;
                } else if (UserModel.EMAIL.equals(name)) {
                    return email;
                } else if (UserModel.USERNAME.equals(name)) {
                    return username;
                }
                return super.getFirstAttribute(name);
            }

            @Override
            public Stream<String> getAttributeStream(String name) {
                if (UserModel.LAST_NAME.equals(name)) {
                    return Stream.of(lastName);
                } else if (UserModel.FIRST_NAME.equals(name)) {
                    return Stream.of(firstName);
                } else if (UserModel.EMAIL.equals(name)) {
                    return Stream.of(email);
                } else if (UserModel.USERNAME.equals(name)) {
                    return Stream.of(username);
                } else if ("postal_code".equals(name) && postalCode != null && postalCode.length > 0) {
                    return Stream.of(postalCode);
                } else if ("street".equals(name) && street != null) {
                    return Stream.of(street);
                } else {
                    return Stream.empty();
                }
            }
        };
        return LDAPUtils.addUserToLDAP(ldapProvider, realm, helperUser);
    }

    public static LDAPObject addLdapOU(LDAPStorageProvider ldapProvider, String name) {
        LDAPObject ldapObject = new LDAPObject();
        ldapObject.setRdnAttributeName("ou");
        ldapObject.setObjectClasses(Collections.singletonList("organizationalUnit"));
        ldapObject.setSingleAttribute("ou", name);
        LDAPDn dn = LDAPDn.fromString(ldapProvider.getLdapIdentityStore().getConfig().getUsersDn());
        dn.addFirst("ou", name);
        ldapObject.setDn(dn);
        ldapProvider.getLdapIdentityStore().add(ldapObject);
        return ldapObject;
    }

    public static void updateLDAPPassword(LDAPStorageProvider ldapProvider, LDAPObject ldapUser, String password) {
        ldapProvider.getLdapIdentityStore().updatePassword(ldapUser, password, null);

        // Enable MSAD user through userAccountControls
        if (ldapProvider.getLdapIdentityStore().getConfig().isActiveDirectory()) {
            ldapUser.setSingleAttribute(LDAPConstants.USER_ACCOUNT_CONTROL, "512");
            ldapProvider.getLdapIdentityStore().update(ldapUser);
        }
    }

    public static ComponentModel getLdapProviderModel(RealmModel realm) {
        return realm.getComponentsStream(realm.getId(), UserStorageProvider.class.getName())
                .filter(component -> Objects.equals(component.getProviderId(), LDAPStorageProviderFactory.PROVIDER_NAME))
                .findFirst()
                .orElse(null);
    }

    public static LDAPStorageProvider getLdapProvider(KeycloakSession keycloakSession, ComponentModel ldapFedModel) {
        return (LDAPStorageProvider)keycloakSession.getProvider(UserStorageProvider.class, ldapFedModel);
    }


    // CRUD model mappers

    public static void addZipCodeLDAPMapper(RealmModel realm, ComponentModel providerModel) {
        addUserAttributeMapper(realm, providerModel, "zipCodeMapper", "postal_code", LDAPConstants.POSTAL_CODE);
    }

    public static ComponentModel addUserAttributeMapper(RealmModel realm, ComponentModel providerModel, String mapperName, String userModelAttributeName, String ldapAttributeName) {
        ComponentModel mapperModel = KeycloakModelUtils.createComponentModel(mapperName, providerModel.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, userModelAttributeName,
                UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, ldapAttributeName,
                UserAttributeLDAPStorageMapper.READ_ONLY, "false",
                UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "false");
        return realm.addComponentModel(mapperModel);
    }

    public static void addOrUpdateRoleLDAPMappers(RealmModel realm, ComponentModel providerModel, LDAPGroupMapperMode mode) {
        ComponentModel mapperModel = getSubcomponentByName(realm, providerModel, "realmRolesMapper");
        if (mapperModel != null) {
            mapperModel.getConfig().putSingle(RoleMapperConfig.MODE, mode.toString());
            realm.updateComponent(mapperModel);
        } else {
            String baseDn = providerModel.getConfig().getFirst(LDAPConstants.BASE_DN);
            mapperModel = KeycloakModelUtils.createComponentModel("realmRolesMapper", providerModel.getId(), RoleLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    RoleMapperConfig.ROLES_DN, "ou=RealmRoles," + baseDn,
                    RoleMapperConfig.USE_REALM_ROLES_MAPPING, "true",
                    RoleMapperConfig.MODE, mode.toString());
            realm.addComponentModel(mapperModel);
        }

        mapperModel = getSubcomponentByName(realm, providerModel, "financeRolesMapper");
        if (mapperModel != null) {
            mapperModel.getConfig().putSingle(RoleMapperConfig.MODE, mode.toString());
            realm.updateComponent(mapperModel);
        } else {
            String baseDn = providerModel.getConfig().getFirst(LDAPConstants.BASE_DN);
            mapperModel = KeycloakModelUtils.createComponentModel("financeRolesMapper", providerModel.getId(), RoleLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    RoleMapperConfig.ROLES_DN, "ou=FinanceRoles," + baseDn,
                    RoleMapperConfig.USE_REALM_ROLES_MAPPING, "false",
                    RoleMapperConfig.CLIENT_ID, "finance",
                    RoleMapperConfig.MODE, mode.toString());
            realm.addComponentModel(mapperModel);
        }
    }

    public static ComponentModel getSubcomponentByName(RealmModel realm, ComponentModel providerModel, String name) {
        return realm.getComponentsStream(providerModel.getId(), LDAPStorageMapper.class.getName())
                .filter(component -> Objects.equals(name, component.getName()))
                .findFirst()
                .orElse(null);
    }

    public static void addOrUpdateGroupMapper(RealmModel realm, ComponentModel providerModel, LDAPGroupMapperMode mode, String descriptionAttrName, String... otherConfigOptions) {
        ComponentModel mapperModel = getSubcomponentByName(realm, providerModel, "groupsMapper");
        if (mapperModel != null) {
            mapperModel.getConfig().putSingle(GroupMapperConfig.MODE, mode.toString());
            updateGroupMapperConfigOptions(mapperModel, otherConfigOptions);
            realm.updateComponent(mapperModel);
        } else {
            String baseDn = providerModel.getConfig().getFirst(LDAPConstants.BASE_DN);
            mapperModel = KeycloakModelUtils.createComponentModel("groupsMapper", providerModel.getId(), GroupLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    GroupMapperConfig.GROUPS_DN, "ou=Groups," + baseDn,
                    GroupMapperConfig.MAPPED_GROUP_ATTRIBUTES, descriptionAttrName,
                    GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "true",
                    GroupMapperConfig.MODE, mode.toString(),
                    GroupMapperConfig.LDAP_GROUPS_PATH, "/");
            updateGroupMapperConfigOptions(mapperModel, otherConfigOptions);
            realm.addComponentModel(mapperModel);
        }
    }

    public static void addOrUpdateRoleMapper(RealmModel realm, ComponentModel providerModel, LDAPGroupMapperMode mode, String... otherConfigOptions) {
        ComponentModel mapperModel = getSubcomponentByName(realm, providerModel, "rolesMapper");
        if (mapperModel != null) {
            mapperModel.getConfig().putSingle(GroupMapperConfig.MODE, mode.toString());
            updateGroupMapperConfigOptions(mapperModel, otherConfigOptions);
            realm.updateComponent(mapperModel);
        } else {
            String baseDn = providerModel.getConfig().getFirst(LDAPConstants.BASE_DN);
            mapperModel = KeycloakModelUtils.createComponentModel("rolesMapper", providerModel.getId(), RoleLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    RoleMapperConfig.ROLES_DN, "ou=Groups," + baseDn,
                    RoleMapperConfig.USE_REALM_ROLES_MAPPING, "true",
                    GroupMapperConfig.MODE, mode.toString());
            updateGroupMapperConfigOptions(mapperModel, otherConfigOptions);
            realm.addComponentModel(mapperModel);
        }
    }

    public static void updateGroupMapperConfigOptions(ComponentModel mapperModel, String... configOptions) {
        for (int i=0 ; i<configOptions.length ; i+=2) {
            String cfgName = configOptions[i];
            String cfgValue = configOptions[i+1];
            mapperModel.getConfig().putSingle(cfgName, cfgValue);
        }
    }

    // End CRUD model mappers

    public static void syncRolesFromLDAP(RealmModel realm, LDAPStorageProvider ldapProvider, ComponentModel providerModel) {
        ComponentModel mapperModel = getSubcomponentByName(realm, providerModel, "realmRolesMapper");
        RoleLDAPStorageMapper roleMapper = getRoleMapper(mapperModel, ldapProvider, realm);

        roleMapper.syncDataFromFederationProviderToKeycloak(realm);

        mapperModel = getSubcomponentByName(realm, providerModel, "financeRolesMapper");
        roleMapper = getRoleMapper(mapperModel, ldapProvider, realm);
        roleMapper.syncDataFromFederationProviderToKeycloak(realm);
    }

    public static void removeAllLDAPUsers(LDAPStorageProvider ldapProvider, RealmModel realm) {
        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(ldapProvider, realm)) {
            List<LDAPObject> allUsers = ldapQuery.getResultList();

            for (LDAPObject ldapUser : allUsers) {
                ldapStore.remove(ldapUser);
            }
        }
    }
    
    public static void removeLDAPUserByUsername(LDAPStorageProvider ldapProvider, RealmModel realm, LDAPConfig config, String username) {
        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(ldapProvider, realm)) {
            List<LDAPObject> allUsers = ldapQuery.getResultList();

            // This is ugly, we are iterating over the entire set of ldap users and deleting the one where the username matches.  TODO: Find a better way!
            for (LDAPObject ldapUser : allUsers) {
                if (username.equals(LDAPUtils.getUsername(ldapUser, config))) {
                    ldapStore.remove(ldapUser);
                }
            }
        }
    }
    
    public static void removeAllLDAPRoles(KeycloakSession session, RealmModel appRealm, ComponentModel ldapModel, String mapperName) {
        ComponentModel mapperModel = getSubcomponentByName(appRealm, ldapModel, mapperName);
        LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
        try (LDAPQuery roleQuery = getRoleMapper(mapperModel, ldapProvider, appRealm).createRoleQuery(false)) {
            List<LDAPObject> ldapRoles = roleQuery.getResultList();
            for (LDAPObject ldapRole : ldapRoles) {
                ldapProvider.getLdapIdentityStore().remove(ldapRole);
            }
        }
    }

    public static void removeAllLDAPGroups(KeycloakSession session, RealmModel appRealm, ComponentModel ldapModel, String mapperName) {
        ComponentModel mapperModel = getSubcomponentByName(appRealm, ldapModel, mapperName);
        LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
        LDAPQuery query = null;
        if (GroupLDAPStorageMapperFactory.PROVIDER_ID.equals(mapperModel.getProviderId())) {
            query = getGroupMapper(mapperModel, ldapProvider, appRealm).createGroupQuery(false);
        } else {
            query = getRoleMapper(mapperModel, ldapProvider, appRealm).createRoleQuery(false);
        }
        try (LDAPQuery roleQuery = query) {
            List<LDAPObject> ldapRoles = roleQuery.getResultList();
            for (LDAPObject ldapRole : ldapRoles) {
                ldapProvider.getLdapIdentityStore().remove(ldapRole);
            }
        }
    }

    public static void createLDAPRole(KeycloakSession session, RealmModel appRealm, ComponentModel ldapModel, String mapperName, String roleName) {
        ComponentModel mapperModel = getSubcomponentByName(appRealm, ldapModel, mapperName);
        LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
        getRoleMapper(mapperModel, ldapProvider, appRealm).createLDAPRole(roleName);
    }

    public static LDAPObject createLDAPGroup(KeycloakSession session, RealmModel appRealm, ComponentModel ldapModel, String groupName, String... additionalAttrs) {
        return createLDAPGroup("groupsMapper", session, appRealm, ldapModel, groupName, additionalAttrs);
    }

    public static LDAPObject createLDAPGroup(String mapperName, KeycloakSession session, RealmModel appRealm, ComponentModel ldapModel, String groupName, String... additionalAttrs) {
        ComponentModel mapperModel = getSubcomponentByName(appRealm, ldapModel, mapperName);
        LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

        Map<String, Set<String>> additAttrs = new HashMap<>();
        for (int i=0 ; i<additionalAttrs.length ; i+=2) {
            String attrName = additionalAttrs[i];
            String attrValue = additionalAttrs[i+1];
            additAttrs.put(attrName, Collections.singleton(attrValue));
        }

        if (GroupLDAPStorageMapperFactory.PROVIDER_ID.equals(mapperModel.getProviderId())) {
            return getGroupMapper(mapperModel, ldapProvider, appRealm).createLDAPGroup(groupName, additAttrs);
        } else {
            return getRoleMapper(mapperModel, ldapProvider, appRealm).createLDAPRole(groupName);
        }
    }

    public static LDAPObject updateLDAPGroup(KeycloakSession session, RealmModel appRealm, ComponentModel ldapModel, LDAPObject ldapObject) {
        ComponentModel mapperModel = getSubcomponentByName(appRealm, ldapModel, "groupsMapper");
        LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

        return getGroupMapper(mapperModel, ldapProvider, appRealm).updateLDAPGroup(ldapObject);
    }

    public static GroupLDAPStorageMapper getGroupMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, RealmModel realm) {
        return new GroupLDAPStorageMapper(mapperModel, ldapProvider, new GroupLDAPStorageMapperFactory());
    }

    public static RoleLDAPStorageMapper getRoleMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, RealmModel realm) {
        return new RoleLDAPStorageMapper(mapperModel, ldapProvider, new RoleLDAPStorageMapperFactory());
    }


    public static String getGroupDescriptionLDAPAttrName(LDAPStorageProvider ldapProvider) {
        return ldapProvider.getLdapIdentityStore().getConfig().isActiveDirectory() ? "displayName" : "description";
    }
}
