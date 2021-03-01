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

package org.keycloak.testsuite.federation.ldap.noimport;

import java.util.Collections;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.ldap.LDAPProvidersIntegrationTest;
import org.keycloak.testsuite.federation.ldap.LDAPTestAsserts;
import org.keycloak.testsuite.federation.ldap.LDAPTestContext;
import org.keycloak.testsuite.util.LDAPTestUtils;


/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPProvidersIntegrationNoImportTest extends LDAPProvidersIntegrationTest {


    @Override
    protected boolean isImportEnabled() {
        return false;
    }


    @Override
    protected void assertFederatedUserLink(UserRepresentation user) {
        StorageId storageId = new StorageId(user.getId());
        Assert.assertFalse(storageId.isLocal());
        Assert.assertEquals(ldapModelId, storageId.getProviderId());

        // TODO: It should be possibly LDAP_ID (LDAP UUID) used as an externalId inside storageId...
        Assert.assertEquals(storageId.getExternalId(), user.getUsername());
        Assert.assertNull(user.getFederationLink());
    }


    // No sense to test this in no-import mode
    @Test
    @Ignore
    @Override
    public void testRemoveImportedUsers() {
    }


    @Test
    @Override
    public void testSearch() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username1", "John1", "Doel1", "user1@email.org", null, "121");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username2", "John2", "Doel2", "user2@email.org", null, "122");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username3", "John3", "Doel3", "user3@email.org", null, "123");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username4", "John4", "Doel4", "user4@email.org", null, "124");

            // search by username
            UserModel user = session.users().searchForUserStream(appRealm, "username1").findFirst().get();
            LDAPTestAsserts.assertLoaded(user, "username1", "John1", "Doel1", "user1@email.org", "121");

            // search by email
            user = session.users().searchForUserStream(appRealm, "user2@email.org").findFirst().get();
            LDAPTestAsserts.assertLoaded(user, "username2", "John2", "Doel2", "user2@email.org", "122");

            // search by lastName
            user = session.users().searchForUserStream(appRealm, "Doel3").findFirst().get();
            LDAPTestAsserts.assertLoaded(user, "username3", "John3", "Doel3", "user3@email.org", "123");

            // search by firstName + lastName
            user = session.users().searchForUserStream(appRealm, "John4 Doel4").findFirst().get();
            LDAPTestAsserts.assertLoaded(user, "username4", "John4", "Doel4", "user4@email.org", "124");
        });
    }


    // No need to test this in no-import mode. There won't be any users in localStorage after LDAP search
    @Test
    @Ignore
    @Override
    public void testSearchByAttributes() {
    }


    @Test
    @Override
    public void testSearchWithCustomLDAPFilter() {
        // Add custom filter for searching users
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(|(mail=user5@email.org)(mail=user6@email.org))");
            appRealm.updateComponent(ctx.getLdapModel());
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username5", "John5", "Doel5", "user5@email.org", null, "125");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username6", "John6", "Doel6", "user6@email.org", null, "126");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username7", "John7", "Doel7", "user7@email.org", null, "127");

            // search by email
            UserModel user = session.users().searchForUserStream(appRealm, "user5@email.org").findFirst().get();
            LDAPTestAsserts.assertLoaded(user, "username5", "John5", "Doel5", "user5@email.org", "125");

            user = session.users().searchForUserStream(appRealm, "John6 Doel6").findFirst().get();
            LDAPTestAsserts.assertLoaded(user, "username6", "John6", "Doel6", "user6@email.org", "126");

            Assert.assertEquals(0, session.users().searchForUserStream(appRealm, "user7@email.org").count());
            Assert.assertEquals(0, session.users().searchForUserStream(appRealm, "John7 Doel7").count());

            // Remove custom filter
            ctx.getLdapModel().getConfig().remove(LDAPConstants.CUSTOM_USER_SEARCH_FILTER);
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }


    @Test
    @Override
    // Unsynced mode doesn't have much sense in no-import. So it is not allowed at the configuration level
    public void testUnsynced() throws Exception {
        ComponentResource ldapProviderResource = testRealm().components().component(ldapModelId);
        ComponentRepresentation ldapProviderRep = ldapProviderResource.toRepresentation();
        String currentEditMode = ldapProviderRep.getConfig().getFirst(LDAPConstants.EDIT_MODE);
        Assert.assertEquals(UserStorageProvider.EditMode.WRITABLE.toString(), currentEditMode);

        // Try update editMode to UNSYNCED. It should not work as UNSYNCED with no-import is not allowed
        ldapProviderRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.toString());
        try {
            ldapProviderResource.update(ldapProviderRep);
            Assert.fail("Not expected to successfully update provider");
        } catch (BadRequestException bre) {
            // Expected
        }

        // Try to set editMode to WRITABLE should work
        ldapProviderRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, currentEditMode);
        ldapProviderResource.update(ldapProviderRep);
    }


    @Test
    @Override
    @Ignore // Unlinking users doesn't have much sense in no-import
    public void zzTestUnlinkUsers() {
    }


    @Test
    public void testFullNameMapperWriteOnly() {
        ComponentRepresentation firstNameMapperRep = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // assert that user "fullnameUser" is not in local DB
            Assert.assertNull(session.users().getUserByUsername(appRealm, "fullname"));

            // Add the user with some fullName into LDAP directly. Ensure that fullName is saved into "cn" attribute in LDAP (currently mapped to model firstName)
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "fullname", "James Dee", "Dee", "fullname@email.org", null, "4578");

            // add fullname mapper to the provider and remove "firstNameMapper". For this test, we will simply map full name to the LDAP attribute, which was before firstName ( "givenName" on active directory, "cn" on other LDAP servers)
            ComponentModel firstNameMapper =  LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "first name");
            String ldapFirstNameAttributeName = firstNameMapper.getConfig().getFirst(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE);
            appRealm.removeComponent(firstNameMapper);

            ComponentRepresentation firstNameMapperRepp = ModelToRepresentation.toRepresentation(session, firstNameMapper, true);

            ComponentModel fullNameMapperModel = KeycloakModelUtils.createComponentModel("full name", ldapModel.getId(), FullNameLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE, ldapFirstNameAttributeName,
                    FullNameLDAPStorageMapper.READ_ONLY, "false");
            appRealm.addComponentModel(fullNameMapperModel);

            return firstNameMapperRepp;
        }, ComponentRepresentation.class);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            LDAPTestAsserts.assertUserImported(session.users(), appRealm, "fullname", "James", "Dee", "fullname@email.org", "4578");

            // change mapper to writeOnly
            ComponentModel fullNameMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "full name");
            fullNameMapperModel.getConfig().putSingle(FullNameLDAPStorageMapper.WRITE_ONLY, "true");
            appRealm.updateComponent(fullNameMapperModel);
        });

        // User will be changed in LDAP too
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel fullnameUser = session.users().getUserByUsername(appRealm, "fullname");
            fullnameUser.setFirstName("James2");
            fullnameUser.setLastName("Dee2");
        });

        // Assert changed user available in Keycloak, but his firstName is null (due the fullnameMapper is write-only and firstName mapper is removed)
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            LDAPTestAsserts.assertUserImported(session.users(), appRealm, "fullname", null, "Dee2", "fullname@email.org", "4578");

            // Remove "fullnameUser" to assert he is removed from LDAP. Revert mappers to previous state
            UserModel fullnameUser = session.users().getUserByUsername(appRealm, "fullname");
            session.users().removeUser(appRealm, fullnameUser);

            // Revert mappers
            ComponentModel fullNameMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "full name");
            appRealm.removeComponent(fullNameMapperModel);
        });

        firstNameMapperRep.setId(null);
        Response response = testRealm().components().add(firstNameMapperRep);
        Assert.assertEquals(201, response.getStatus());
        response.close();
    }

    // Tests that attempt to change some user attributes, which are not mapped to LDAP, will fail
    @Test
    public void testImpossibleToChangeNonLDAPMappedAttributes() {
        UserResource john = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");

        UserRepresentation johnRep = john.toRepresentation();
        String firstNameOrig = johnRep.getFirstName();
        String lastNameOrig = johnRep.getLastName();
        String emailOrig = johnRep.getEmail();
        String postalCodeOrig = johnRep.getAttributes().get("postal_code").get(0);

        try {
            // Attempt to disable user should fail
            try {
                johnRep.setFirstName("John2");
                johnRep.setLastName("Doe2");
                johnRep.setEnabled(false);

                john.update(johnRep);
                Assert.fail("Not supposed to successfully update 'enabled' state of the user");
            } catch (BadRequestException bre) {
                // Expected
            }

            // Attempt to set requiredAction to the user should fail
            try {
                johnRep = john.toRepresentation();
                johnRep.setRequiredActions(Collections.singletonList(UserModel.RequiredAction.CONFIGURE_TOTP.toString()));
                john.update(johnRep);
                Assert.fail("Not supposed to successfully add requiredAction to the user");
            } catch (BadRequestException bre) {
                // Expected
            }

            // Attempt to add some new attribute should fail
            try {
                johnRep = john.toRepresentation();
                johnRep.singleAttribute("foo", "bar");
                john.update(johnRep);
                Assert.fail("Not supposed to successfully add attribute to the user");
            } catch (BadRequestException bre) {
                // Expected
            }

            // Attempt to update firstName, lastName and postal_code should be successful. All those attributes are mapped to LDAP
            johnRep = john.toRepresentation();
            johnRep.setFirstName("John2");
            johnRep.setLastName("Doe2");
            johnRep.singleAttribute("postal_code", "654321");
            john.update(johnRep);

            johnRep = john.toRepresentation();
            Assert.assertEquals("John2", johnRep.getFirstName());
            Assert.assertEquals("Doe2", johnRep.getLastName());
            Assert.assertEquals("654321", johnRep.getAttributes().get("postal_code").get(0));
        } finally {
            // Revert
            johnRep.setFirstName(firstNameOrig);
            johnRep.setLastName(lastNameOrig);
            johnRep.singleAttribute("postal_code", postalCodeOrig);
            john.update(johnRep);
            Assert.assertEquals(firstNameOrig, johnRep.getFirstName());
            Assert.assertEquals(lastNameOrig, johnRep.getLastName());
            Assert.assertEquals(emailOrig, johnRep.getEmail());
            Assert.assertEquals(postalCodeOrig, johnRep.getAttributes().get("postal_code").get(0));
        }
    }

    // No need to test this in no-import mode. There won't be any users in localStorage.
    @Test
    @Ignore
    @Override
    public void updateLDAPUsernameTest() {
    }
}
