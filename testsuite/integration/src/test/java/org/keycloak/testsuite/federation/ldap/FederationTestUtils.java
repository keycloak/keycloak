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

package org.keycloak.testsuite.federation.ldap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.federation.ldap.mappers.membership.group.GroupLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.federation.ldap.mappers.membership.role.RoleLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.membership.role.RoleLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.membership.group.GroupLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.membership.role.RoleMapperConfig;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.representations.idm.CredentialRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FederationTestUtils {

    public static UserModel addLocalUser(KeycloakSession session, RealmModel realm, String username, String email, String password) {
        UserModel user = session.userStorage().addUser(realm, username);
        user.setEmail(email);
        user.setEnabled(true);

        UserCredentialModel creds = new UserCredentialModel();
        creds.setType(CredentialRepresentation.PASSWORD);
        creds.setValue(password);

        user.updateCredential(creds);
        return user;
    }

    public static LDAPObject addLDAPUser(LDAPFederationProvider ldapProvider, RealmModel realm, final String username,
                                            final String firstName, final String lastName, final String email, final String street, final String... postalCode) {
        UserModel helperUser = new UserModelDelegate(null) {

            @Override
            public String getUsername() {
                return username;
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
            public String getEmail() {
                return email;
            }

            @Override
            public List<String> getAttribute(String name) {
                if ("postal_code".equals(name) && postalCode != null && postalCode.length > 0) {
                    return Arrays.asList(postalCode);
                } else if ("street".equals(name) && street != null) {
                    return Collections.singletonList(street);
                } else {
                    return Collections.emptyList();
                }
            }
        };
        return LDAPUtils.addUserToLDAP(ldapProvider, realm, helperUser);
    }

    public static void updateLDAPPassword(LDAPFederationProvider ldapProvider, LDAPObject ldapUser, String password) {
        ldapProvider.getLdapIdentityStore().updatePassword(ldapUser, password);

        // Enable MSAD user through userAccountControls
        if (ldapProvider.getLdapIdentityStore().getConfig().isActiveDirectory()) {
            ldapUser.setSingleAttribute(LDAPConstants.USER_ACCOUNT_CONTROL, "512");
            ldapProvider.getLdapIdentityStore().update(ldapUser);
        }
    }

    public static LDAPFederationProvider getLdapProvider(KeycloakSession keycloakSession, UserFederationProviderModel ldapFedModel) {
        LDAPFederationProviderFactory ldapProviderFactory = (LDAPFederationProviderFactory) keycloakSession.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, ldapFedModel.getProviderName());
        return ldapProviderFactory.getInstance(keycloakSession, ldapFedModel);
    }

    public static void assertUserImported(UserProvider userProvider, RealmModel realm, String username, String expectedFirstName, String expectedLastName, String expectedEmail, String expectedPostalCode) {
        UserModel user = userProvider.getUserByUsername(username, realm);
        Assert.assertNotNull(user);
        Assert.assertEquals(expectedFirstName, user.getFirstName());
        Assert.assertEquals(expectedLastName, user.getLastName());
        Assert.assertEquals(expectedEmail, user.getEmail());
        Assert.assertEquals(expectedPostalCode, user.getFirstAttribute("postal_code"));
    }


    // CRUD model mappers

    public static void addZipCodeLDAPMapper(RealmModel realm, UserFederationProviderModel providerModel) {
        addUserAttributeMapper(realm, providerModel, "zipCodeMapper", "postal_code", LDAPConstants.POSTAL_CODE);
    }

    public static UserFederationMapperModel addUserAttributeMapper(RealmModel realm, UserFederationProviderModel providerModel, String mapperName, String userModelAttributeName, String ldapAttributeName) {
        UserFederationMapperModel mapperModel = KeycloakModelUtils.createUserFederationMapperModel(mapperName, providerModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, userModelAttributeName,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, ldapAttributeName,
                UserAttributeLDAPFederationMapper.READ_ONLY, "false",
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "false");
        return realm.addUserFederationMapper(mapperModel);
    }

    public static void addOrUpdateRoleLDAPMappers(RealmModel realm, UserFederationProviderModel providerModel, LDAPGroupMapperMode mode) {
        UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(providerModel.getId(), "realmRolesMapper");
        if (mapperModel != null) {
            mapperModel.getConfig().put(RoleMapperConfig.MODE, mode.toString());
            realm.updateUserFederationMapper(mapperModel);
        } else {
            String baseDn = providerModel.getConfig().get(LDAPConstants.BASE_DN);
            mapperModel = KeycloakModelUtils.createUserFederationMapperModel("realmRolesMapper", providerModel.getId(), RoleLDAPFederationMapperFactory.PROVIDER_ID,
                    RoleMapperConfig.ROLES_DN, "ou=RealmRoles," + baseDn,
                    RoleMapperConfig.USE_REALM_ROLES_MAPPING, "true",
                    RoleMapperConfig.MODE, mode.toString());
            realm.addUserFederationMapper(mapperModel);
        }

        mapperModel = realm.getUserFederationMapperByName(providerModel.getId(), "financeRolesMapper");
        if (mapperModel != null) {
            mapperModel.getConfig().put(RoleMapperConfig.MODE, mode.toString());
            realm.updateUserFederationMapper(mapperModel);
        } else {
            String baseDn = providerModel.getConfig().get(LDAPConstants.BASE_DN);
            mapperModel = KeycloakModelUtils.createUserFederationMapperModel("financeRolesMapper", providerModel.getId(), RoleLDAPFederationMapperFactory.PROVIDER_ID,
                    RoleMapperConfig.ROLES_DN, "ou=FinanceRoles," + baseDn,
                    RoleMapperConfig.USE_REALM_ROLES_MAPPING, "false",
                    RoleMapperConfig.CLIENT_ID, "finance",
                    RoleMapperConfig.MODE, mode.toString());
            realm.addUserFederationMapper(mapperModel);
        }
    }

    public static void addOrUpdateGroupMapper(RealmModel realm, UserFederationProviderModel providerModel, LDAPGroupMapperMode mode, String descriptionAttrName, String... otherConfigOptions) {
        UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(providerModel.getId(), "groupsMapper");
        if (mapperModel != null) {
            mapperModel.getConfig().put(GroupMapperConfig.MODE, mode.toString());
            updateGroupMapperConfigOptions(mapperModel, otherConfigOptions);
            realm.updateUserFederationMapper(mapperModel);
        } else {
            String baseDn = providerModel.getConfig().get(LDAPConstants.BASE_DN);
            mapperModel = KeycloakModelUtils.createUserFederationMapperModel("groupsMapper", providerModel.getId(), GroupLDAPFederationMapperFactory.PROVIDER_ID,
                    GroupMapperConfig.GROUPS_DN, "ou=Groups," + baseDn,
                    GroupMapperConfig.MAPPED_GROUP_ATTRIBUTES, descriptionAttrName,
                    GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "true",
                    GroupMapperConfig.MODE, mode.toString());
            updateGroupMapperConfigOptions(mapperModel, otherConfigOptions);
            realm.addUserFederationMapper(mapperModel);
        }
    }

    public static void updateGroupMapperConfigOptions(UserFederationMapperModel mapperModel, String... configOptions) {
        for (int i=0 ; i<configOptions.length ; i+=2) {
            String cfgName = configOptions[i];
            String cfgValue = configOptions[i+1];
            mapperModel.getConfig().put(cfgName, cfgValue);
        }
    }

    // End CRUD model mappers

    public static void syncRolesFromLDAP(RealmModel realm, LDAPFederationProvider ldapProvider, UserFederationProviderModel providerModel) {
        UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(providerModel.getId(), "realmRolesMapper");
        RoleLDAPFederationMapper roleMapper = getRoleMapper(mapperModel, ldapProvider, realm);

        roleMapper.syncDataFromFederationProviderToKeycloak();

        mapperModel = realm.getUserFederationMapperByName(providerModel.getId(), "financeRolesMapper");
        roleMapper = getRoleMapper(mapperModel, ldapProvider, realm);
        roleMapper.syncDataFromFederationProviderToKeycloak();
    }

    public static void removeAllLDAPUsers(LDAPFederationProvider ldapProvider, RealmModel realm) {
        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(ldapProvider, realm);
        List<LDAPObject> allUsers = ldapQuery.getResultList();

        for (LDAPObject ldapUser : allUsers) {
            ldapStore.remove(ldapUser);
        }
    }

    public static void removeAllLDAPRoles(KeycloakSession session, RealmModel appRealm, UserFederationProviderModel ldapModel, String mapperName) {
        UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), mapperName);
        LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
        LDAPQuery roleQuery = getRoleMapper(mapperModel, ldapProvider, appRealm).createRoleQuery();
        List<LDAPObject> ldapRoles = roleQuery.getResultList();
        for (LDAPObject ldapRole : ldapRoles) {
            ldapProvider.getLdapIdentityStore().remove(ldapRole);
        }
    }

    public static void removeAllLDAPGroups(KeycloakSession session, RealmModel appRealm, UserFederationProviderModel ldapModel, String mapperName) {
        UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), mapperName);
        LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
        LDAPQuery roleQuery = getGroupMapper(mapperModel, ldapProvider, appRealm).createGroupQuery();
        List<LDAPObject> ldapRoles = roleQuery.getResultList();
        for (LDAPObject ldapRole : ldapRoles) {
            ldapProvider.getLdapIdentityStore().remove(ldapRole);
        }
    }

    public static void createLDAPRole(KeycloakSession session, RealmModel appRealm, UserFederationProviderModel ldapModel, String mapperName, String roleName) {
        UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), mapperName);
        LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
        getRoleMapper(mapperModel, ldapProvider, appRealm).createLDAPRole(roleName);
    }

    public static LDAPObject createLDAPGroup(KeycloakSession session, RealmModel appRealm, UserFederationProviderModel ldapModel, String groupName, String... additionalAttrs) {
        UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
        LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

        Map<String, Set<String>> additAttrs = new HashMap<>();
        for (int i=0 ; i<additionalAttrs.length ; i+=2) {
            String attrName = additionalAttrs[i];
            String attrValue = additionalAttrs[i+1];
            additAttrs.put(attrName, Collections.singleton(attrValue));
        }

        return getGroupMapper(mapperModel, ldapProvider, appRealm).createLDAPGroup(groupName, additAttrs);
    }

    public static GroupLDAPFederationMapper getGroupMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, RealmModel realm) {
        return new GroupLDAPFederationMapper(mapperModel, ldapProvider, realm, new GroupLDAPFederationMapperFactory());
    }

    public static RoleLDAPFederationMapper getRoleMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, RealmModel realm) {
        return new RoleLDAPFederationMapper(mapperModel, ldapProvider, realm, new RoleLDAPFederationMapperFactory());
    }


    public static void assertSyncEquals(UserFederationSyncResult syncResult, int expectedAdded, int expectedUpdated, int expectedRemoved, int expectedFailed) {
        Assert.assertEquals(expectedAdded, syncResult.getAdded());
        Assert.assertEquals(expectedUpdated, syncResult.getUpdated());
        Assert.assertEquals(expectedRemoved, syncResult.getRemoved());
        Assert.assertEquals(expectedFailed, syncResult.getFailed());
    }
}
