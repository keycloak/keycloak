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

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPBinaryAttributesTest extends AbstractLDAPTest {

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

            // Delete all LDAP users and add some new for testing
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

//            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
//            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");
//
//            LDAPObject existing = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "existing", "Existing", "Foo", "existing@email.org", null, "5678");

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);

        });
    }


    private static final String JPEG_PHOTO_BASE64 = "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";


    @After
    public void after() {
        ComponentRepresentation jpegMapper = adminClient.realm("test").components().query(ldapModelId, LDAPStorageMapper.class.getName(), "jpeg-mapper").get(0);
        adminClient.realm("test").components().component(jpegMapper.getId()).remove();
    }


    // Test invalid mapper configuration - validation exception thrown
    @Test
    public void test01InvalidMapperConfiguration() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel ldapComponentMapper = LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "jpeg-mapper", LDAPConstants.JPEG_PHOTO, LDAPConstants.JPEG_PHOTO);

            ldapComponentMapper.put(UserAttributeLDAPStorageMapper.IS_BINARY_ATTRIBUTE, true);
            try {
                appRealm.updateComponent(ldapComponentMapper);
                Assert.fail("Not expected to successfully update mapper");
            } catch (ComponentValidationException cve) {
                // Expected
            }

        });
    }


    @Test
    public void test02ReadOnly() {
        String mapperId = addPhotoMapper(testingClient);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Add user directly to LDAP
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            addLDAPUser(ldapFedProvider, appRealm, "johnphoto", "John", "Photo", "john@photo.org", JPEG_PHOTO_BASE64);

            // Set mapper to be read-only
            ComponentModel ldapComponentMapper = appRealm.getComponent(mapperId);
            ldapComponentMapper.put(UserAttributeLDAPStorageMapper.READ_ONLY, true);
            appRealm.updateComponent(ldapComponentMapper);
        });

        // Assert john found
        getUserAndAssertPhoto("johnphoto", true);
    }


    @Test
    public void test03WritableMapper() {
        String mapperId = addPhotoMapper(testingClient);

        // Create user joe with jpegPHoto
        UserRepresentation joe = new UserRepresentation();
        joe.setUsername("joephoto");
        joe.setEmail("joe@photo.org");
        joe.setAttributes(Collections.singletonMap(LDAPConstants.JPEG_PHOTO, Arrays.asList(JPEG_PHOTO_BASE64)));
        Response response = adminClient.realm("test").users().create(joe);
        response.close();


        // Assert he is found including jpegPhoto
        joe = getUserAndAssertPhoto("joephoto", true);

        // Assert that local storage doesn't contain LDAPConstants.JPEG_PHOTO, it should be stored in the LDAP
        String joeId = joe.getId();
        testingClient.server().run(session -> {
            RealmModel test = session.realms().getRealmByName("test");
            UserModel userById = session.userLocalStorage().getUserById(test, joeId);

            assertThat(userById.getAttributes().get(LDAPConstants.JPEG_PHOTO), is(nullValue()));
        });


        joe.getAttributes().remove(LDAPConstants.JPEG_PHOTO);
        adminClient.realm("test").users().get(joe.getId()).update(joe);
        getUserAndAssertPhoto("joephoto", false);
    }


    private static String addPhotoMapper(KeycloakTestingClient testingClient) {
        return testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel ldapComponentMapper = LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "jpeg-mapper", LDAPConstants.JPEG_PHOTO, LDAPConstants.JPEG_PHOTO);

            ldapComponentMapper.put(UserAttributeLDAPStorageMapper.IS_BINARY_ATTRIBUTE, true);
            ldapComponentMapper.put(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, true);
            appRealm.updateComponent(ldapComponentMapper);
            return ldapComponentMapper.getId();
        }, String.class);
    }


    private static LDAPObject addLDAPUser(LDAPStorageProvider ldapProvider, RealmModel realm, final String username,
                                          final String firstName, final String lastName, final String email, String jpegPhoto) {
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
                } else if (LDAPConstants.JPEG_PHOTO.equals(name)) {
                    return Stream.of(jpegPhoto);
                } else {
                    return Stream.empty();
                }
            }
        };
        return LDAPUtils.addUserToLDAP(ldapProvider, realm, helperUser);
    }


    private UserRepresentation getUserAndAssertPhoto(String username, boolean isPhotoExpected) {
        List<UserRepresentation> johns = adminClient.realm("test").users().search(username, 0, 10);
        Assert.assertEquals(1, johns.size());
        UserRepresentation john = johns.get(0);
        Assert.assertEquals(username, john.getUsername());
        Assert.assertTrue(john.getAttributes().containsKey(LDAPConstants.LDAP_ID)); // Doublecheck it's the LDAP mapped user

        if (isPhotoExpected) {
            Assert.assertEquals(JPEG_PHOTO_BASE64, john.getAttributes().get(LDAPConstants.JPEG_PHOTO).get(0));
        } else {
            Assert.assertFalse(john.getAttributes().containsKey(LDAPConstants.JPEG_PHOTO));
        }
        return john;
    }

}
