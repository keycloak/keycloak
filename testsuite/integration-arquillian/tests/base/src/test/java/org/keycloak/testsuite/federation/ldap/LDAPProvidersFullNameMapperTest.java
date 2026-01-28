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

package org.keycloak.testsuite.federation.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:external.Martin.Idel@bosch.io">Martin Idel</a>
 */
public class LDAPProvidersFullNameMapperTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ctx.getLdapModel());
            LDAPTestUtils.addPostalAddressLDAPMapper(appRealm, ctx.getLdapModel());
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);

            // assert that user "fullnameUser" is not in local DB
            Assert.assertNull(session.users().getUserByUsername(appRealm, "fullname"));

            // Add the user with some fullName into LDAP directly. Ensure that fullName is saved into "cn" attribute in LDAP (currently mapped to model firstName)
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);

            // add fullname mapper to the provider and remove "firstNameMapper". For this test, we will simply map full name to the LDAP attribute, which was before firstName ( "givenName" on active directory, "cn" on other LDAP servers)
            ComponentModel firstNameMapper = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "first name");
            String ldapFirstNameAttributeName = firstNameMapper.getConfig().getFirst(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE);
            appRealm.removeComponent(firstNameMapper);

            ComponentModel fullNameMapperModel = KeycloakModelUtils.createComponentModel("full name", ldapModel.getId(), FullNameLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE, ldapFirstNameAttributeName,
                    FullNameLDAPStorageMapper.READ_ONLY, "false");
            appRealm.addComponentModel(fullNameMapperModel);
        });
    }

    @Test
    public void testUpdatingFirstNameAndLastNamePropagatesToFullnameMapper() {

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            MultivaluedHashMap<String, String> otherAttrs = new MultivaluedHashMap<>();
            otherAttrs.put("postalAddress", List.of("123456", "654321"));
            LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "fullname", "James", "Dee", "fullname@email.org", null, otherAttrs, "4578");

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            LDAPTestAsserts.assertUserImported(session.users(), appRealm, "fullname", "James", "Dee", "fullname@email.org", "4578", otherAttrs);
        });

        // Assert user will be changed in LDAP too
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel fullnameUser = session.users().getUserByUsername(appRealm, "fullname");
            fullnameUser.setFirstName("James2");
            fullnameUser.setLastName("Dee2");
            fullnameUser.setAttribute("postalAddress", Arrays.asList("1234", "2345", "3456"));
        });

        // Assert changed user available in Keycloak
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            MultivaluedHashMap<String, String> otherAttrs = new MultivaluedHashMap<>();
            otherAttrs.put("postalAddress", List.of("1234", "2345", "3456"));
            LDAPTestAsserts.assertUserImported(session.users(), appRealm, "fullname", "James2", "Dee2", "fullname@email.org", "4578", otherAttrs);

            // Remove "fullnameUser" to assert he is removed from LDAP.
            UserModel fullnameUser = session.users().getUserByUsername(appRealm, "fullname");
            session.users().removeUser(appRealm, fullnameUser);
        });
    }

    @Test
    public void testUpdatingAttributesWorksEvenWithEmptyAttributes() {

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "fullname", "James", "Dee", "fullname@email.org", null, "4578");

            LDAPTestAsserts.assertUserImported(session.users(), appRealm, "fullname", "James", "Dee", "fullname@email.org", "4578");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel fullnameUser = session.users().getUserByUsername(appRealm, "fullname");
            fullnameUser.setAttribute("myAttribute", Collections.singletonList("test"));
            fullnameUser.setAttribute("myEmptyAttribute", new ArrayList<>());
            fullnameUser.setAttribute("myNullAttribute", null);
            fullnameUser.setAttribute("myAttrThreeValues", Arrays.asList("one", "two", "three"));
        });

        // Assert changed user available in Keycloak
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            LDAPTestAsserts.assertUserImported(session.users(), appRealm, "fullname", "James", "Dee", "fullname@email.org", "4578");

            UserModel fullnameUser = session.users().getUserByUsername(appRealm, "fullname");
            assertThat(fullnameUser.getAttributeStream("myAttribute").collect(Collectors.toList()), contains("test"));
            assertThat(fullnameUser.getAttributeStream("myEmptyAttribute").collect(Collectors.toList()), is(empty()));
            assertThat(fullnameUser.getAttributeStream("myNullAttribute").collect(Collectors.toList()), is(empty()));
            MatcherAssert.assertThat(Arrays.asList("one", "two", "three"),
                Matchers.containsInAnyOrder(fullnameUser.getAttributeStream("myAttrThreeValues").toArray(String[]::new)));

            // Remove "fullnameUser" to prevent conflicts with other tests
            session.users().removeUser(appRealm, fullnameUser);
        });
    }

    // Test for bug https://github.com/keycloak/keycloak/issues/22091
    @Test
    public void testMultiValuedAttributes() {

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "fullname", "James", "Dee", "fullname@email.org", null, "4578");
        });

        // Add multi-attribute value to the user while fullname mapper is used.
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel fullnameUser = session.users().getUserByUsername(appRealm, "fullname");
            fullnameUser.setAttribute("roles", Arrays.asList("role1", "role2"));
        });

        // Assert that multi-valued attribute is set.
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel fullnameUser = session.users().getUserByUsername(appRealm, "fullname");
            Assert.assertEquals(Arrays.asList("role1", "role2"), fullnameUser.getAttributeStream("roles").collect(Collectors.toList()));

            // Remove "fullnameUser" to prevent conflicts with other tests
            session.users().removeUser(appRealm, fullnameUser);
        });
    }
}
