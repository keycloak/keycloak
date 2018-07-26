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

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
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
            UserModel user = session.users().searchForUser("username1", appRealm).get(0);
            LDAPTestAsserts.assertLoaded(user, "username1", "John1", "Doel1", "user1@email.org", "121");

            // search by email
            user = session.users().searchForUser("user2@email.org", appRealm).get(0);
            LDAPTestAsserts.assertLoaded(user, "username2", "John2", "Doel2", "user2@email.org", "122");

            // search by lastName
            user = session.users().searchForUser("Doel3", appRealm).get(0);
            LDAPTestAsserts.assertLoaded(user, "username3", "John3", "Doel3", "user3@email.org", "123");

            // search by firstName + lastName
            user = session.users().searchForUser("John4 Doel4", appRealm).get(0);
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
            UserModel user = session.users().searchForUser("user5@email.org", appRealm).get(0);
            LDAPTestAsserts.assertLoaded(user, "username5", "John5", "Doel5", "user5@email.org", "125");

            user = session.users().searchForUser("John6 Doel6", appRealm).get(0);
            LDAPTestAsserts.assertLoaded(user, "username6", "John6", "Doel6", "user6@email.org", "126");

            Assert.assertTrue(session.users().searchForUser("user7@email.org", appRealm).isEmpty());
            Assert.assertTrue(session.users().searchForUser("John7 Doel7", appRealm).isEmpty());

            // Remove custom filter
            ctx.getLdapModel().getConfig().remove(LDAPConstants.CUSTOM_USER_SEARCH_FILTER);
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }


    @Test
    @Override
    @Ignore // Unsynced mode doesn't have much sense in no-import
    public void testUnsynced() throws Exception {
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
            Assert.assertNull(session.users().getUserByUsername("fullname", appRealm));

            // Add the user with some fullName into LDAP directly. Ensure that fullName is saved into "cn" attribute in LDAP (currently mapped to model firstName)
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(session, appRealm);
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

            UserModel fullnameUser = session.users().getUserByUsername("fullname", appRealm);
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
            UserModel fullnameUser = session.users().getUserByUsername("fullname", appRealm);
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

}
