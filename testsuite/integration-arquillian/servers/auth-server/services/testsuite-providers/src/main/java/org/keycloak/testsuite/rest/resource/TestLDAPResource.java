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

package org.keycloak.testsuite.rest.resource;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapperFactory;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.utils.MediaType;

import static org.keycloak.testsuite.util.LDAPTestUtils.getGroupDescriptionLDAPAttrName;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestLDAPResource {

    private final KeycloakSession session;
    private final RealmModel realm;

    public TestLDAPResource(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }


    /**
     * @param ldapCfg configuration of LDAP provider
     * @param importEnabled specify if LDAP provider will have import enabled
     * @return ID of newly created provider
     */
    @POST
    @Path("/create-ldap-provider")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String createLDAPProvider(Map<String,String> ldapCfg, @QueryParam("import") boolean importEnabled) {
        MultivaluedHashMap<String, String> ldapConfig = toComponentConfig(ldapCfg);
        ldapConfig.putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
        ldapConfig.putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());

        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setLastSync(0);
        model.setChangedSyncPeriod(-1);
        model.setFullSyncPeriod(-1);
        model.setName("test-ldap");
        model.setPriority(0);
        model.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
        model.setConfig(ldapConfig);

        model.setImportEnabled(importEnabled);

        model.setCachePolicy(UserStorageProviderModel.CachePolicy.MAX_LIFESPAN);
        model.setMaxLifespan(600000); // Lifetime is 10 minutes

        ComponentModel ldapModel = realm.addComponentModel(model);
        return ldapModel.getId();
    }


    private static MultivaluedHashMap<String, String> toComponentConfig(Map<String, String> ldapConfig) {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (Map.Entry<String, String> entry : ldapConfig.entrySet()) {
            config.add(entry.getKey(), entry.getValue());

        }
        return config;
    }


    /**
     * Prepare groups LDAP tests. Creates some LDAP mappers as well as some built-in GRoups and users in LDAP
     */
    @POST
    @Path("/configure-groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public void prepareGroupsLDAPTest() {
        LDAPTestUtils.addLocalUser(session, realm, "mary", "mary@test.com", "password-app");
        LDAPTestUtils.addLocalUser(session, realm, "john", "john@test.com", "password-app");

        ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
        LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
        String descriptionAttrName = getGroupDescriptionLDAPAttrName(ldapFedProvider);

        // Add group mapper
        LDAPTestUtils.addOrUpdateGroupMapper(realm, ldapModel, LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName);

        // Remove all LDAP groups
        LDAPTestUtils.removeAllLDAPGroups(session, realm, ldapModel, "groupsMapper");

        // Add some groups for testing
        LDAPObject group1 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "group1", descriptionAttrName, "group1 - description");
        LDAPObject group11 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "group11");
        LDAPObject group12 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "group12", descriptionAttrName, "group12 - description");

        LDAPObject defaultGroup1 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "defaultGroup1", descriptionAttrName, "Default Group1 - description");
        LDAPObject defaultGroup11 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "defaultGroup11");
        LDAPObject defaultGroup12 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "defaultGroup12", descriptionAttrName, "Default Group12 - description");
        LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "Team 2016/2017", descriptionAttrName, "A group with slashes in the name");
        LDAPObject teamChild20182019 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "Team Child 2018/2019", descriptionAttrName, "A child group with slashes in the name");
        LDAPObject teamSubChild20202021 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "Team SubChild 2020/2021", descriptionAttrName, "A sub child group with slashes in the name");
        LDAPObject defaultGroup13 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "defaultGroup13", descriptionAttrName, "Default Group13 - description");
        LDAPObject teamSubChild20222023 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "Team SubChild 2022/2023/A/B/C/D/E", descriptionAttrName, "A sub child group with slashes in the name");
        LDAPObject defaultGroup14 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "defaultGroup14", descriptionAttrName, "Default Group14 - description");
        LDAPObject teamRoot20242025 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "Team Root 2024/2025/A/B/C/D", descriptionAttrName, "A sub child group with slashes in the name");
        LDAPObject defaultGroup15 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "defaultGroup15", descriptionAttrName, "Default Group15 - description");
        LDAPObject teamSubChild20262027 = LDAPTestUtils.createLDAPGroup(session, realm, ldapModel, "Team SubChild 2026/2027", descriptionAttrName, "A sub child group with slashes in the name");

        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, group11);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, group12);

        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", defaultGroup1, defaultGroup11);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", defaultGroup1, defaultGroup12);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", defaultGroup1, teamChild20182019);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", teamChild20182019, teamSubChild20202021);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", defaultGroup13, teamSubChild20222023);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", teamSubChild20222023, defaultGroup14);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", teamRoot20242025, defaultGroup15);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", defaultGroup15, teamSubChild20262027);

        // Sync LDAP groups to Keycloak DB
        ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ldapModel, "groupsMapper");
        new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);

        realm.addDefaultGroup(KeycloakModelUtils.findGroupByPath(realm, "/defaultGroup1/defaultGroup11"));
        realm.addDefaultGroup(KeycloakModelUtils.findGroupByPath(realm, "/defaultGroup1/defaultGroup12"));

        // Delete all LDAP users
        LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, realm);

        // Add some LDAP users for testing
        LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
        LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");

        LDAPObject mary = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "marykeycloak", "Mary", "Kelly", "mary@email.org", null, "5678");
        LDAPTestUtils.updateLDAPPassword(ldapFedProvider, mary, "Password1");

        LDAPObject rob = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "robkeycloak", "Rob", "Brown", "rob@email.org", null, "8910");
        LDAPTestUtils.updateLDAPPassword(ldapFedProvider, rob, "Password1");

        LDAPObject james = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "jameskeycloak", "James", "Brown", "james@email.org", null, "8910");
        LDAPTestUtils.updateLDAPPassword(ldapFedProvider, james, "Password1");
    }

    /**
     * Prepare groups LDAP tests. Creates some LDAP mappers as well as some built-in GRoups and users in LDAP
     */
    @POST
    @Path("/configure-roles")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public void prepareRolesLDAPTest() {
        ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
        LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

        // Add role mapper
        LDAPTestUtils.addOrUpdateRoleMapper(realm, ldapModel, LDAPGroupMapperMode.LDAP_ONLY);

        // Remove all LDAP groups and users
        LDAPTestUtils.removeAllLDAPGroups(session, realm, ldapModel, "rolesMapper");
        LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, realm);

        // Add some LDAP users for testing
        LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
        LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");
        LDAPObject mary = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "marykeycloak", "Mary", "Kelly", "mary@email.org", null, "5678");
        LDAPTestUtils.updateLDAPPassword(ldapFedProvider, mary, "Password1");
        LDAPObject rob = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "robkeycloak", "Rob", "Brown", "rob@email.org", null, "8910");
        LDAPTestUtils.updateLDAPPassword(ldapFedProvider, rob, "Password1");
        LDAPObject james = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "jameskeycloak", "James", "Brown", "james@email.org", null, "8910");
        LDAPTestUtils.updateLDAPPassword(ldapFedProvider, james, "Password1");

         // Add some groups for testing
        LDAPObject group1 = LDAPTestUtils.createLDAPGroup("rolesMapper", session, realm, ldapModel, "group1");
        LDAPObject group2 = LDAPTestUtils.createLDAPGroup("rolesMapper", session, realm, ldapModel, "group2");
        LDAPObject group3 = LDAPTestUtils.createLDAPGroup("rolesMapper", session, realm, ldapModel, "group3");

        // add the users to the groups
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, john);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, mary);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, rob);

        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group2, john);
        LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group2, mary);

        // Sync LDAP groups to Keycloak DB roles
        ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ldapModel, "rolesMapper");
        new RoleLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
    }

    /**
     * Remove specified user directly just from the LDAP server
     */
    @DELETE
    @Path("/remove-ldap-user")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeLDAPUser(@QueryParam("username") String ldapUsername) {
        ComponentModel ldapCompModel = LDAPTestUtils.getLdapProviderModel(realm);
        UserStorageProviderModel ldapModel = new UserStorageProviderModel(ldapCompModel);
        LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

        LDAPTestUtils.removeLDAPUserByUsername(ldapProvider, realm,
                ldapProvider.getLdapIdentityStore().getConfig(), ldapUsername);
    }
}
