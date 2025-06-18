/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.sssd;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.admin.ApiUtil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * <p>The class needs a SSSD working environment with a set of users created.
 * The users to test are provided by the <em>sssd.properties</em> properties
 * file. Currently the users are the following:</p>
 *
 * <pre>
 * kinit admin
 * ipa group-add --desc='test group' testgroup
 * ipa user-add emily --first=Emily --last=Jones --email=emily@jones.com  --password (emily123)
 * ipa group-add-member testgroup --users=emily
 * ipa user-add bart --first=bart --last=bart --email= --password (bart123)
 * ipa user-add david --first=david --last=david --password (david123)
 * ipa user-disable david
 * </pre>
 *
 * @author rmartinc
 */
public class SSSDTest extends AbstractBaseSSSDTest {

    private static final Logger log = Logger.getLogger(SSSDTest.class);

    private static final String DISPLAY_NAME = "Test user federation";
    private static final String REALM_NAME = "test";

    private String SSSDFederationID;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void createUserFederation() {
        ComponentRepresentation userFederation = new ComponentRepresentation();

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        userFederation.setConfig(config);

        userFederation.setName(DISPLAY_NAME);
        userFederation.getConfig().putSingle("priority", "0");
        userFederation.setProviderType(UserStorageProvider.class.getName());
        userFederation.setProviderId(PROVIDER_NAME);

        try (Response response = adminClient.realm(REALM_NAME).components().add(userFederation)) {
            SSSDFederationID = ApiUtil.getCreatedId(response);
        }
    }

    @Test
    public void testInvalidPassword() {
        String username = getUsername();
        log.debug("Testing invalid password for user " + username);

        testLoginFailure(username, "invalid-password");
    }

    @Test
    public void testDisabledUser() {
        String username = getUser(DISABLED_USER);
        Assume.assumeTrue("Ignoring test no disabled user configured", username != null);
        log.debug("Testing disabled user " + username);

        testLoginFailure(username, getPassword(username));
    }

    @Test
    public void testAdmin() {
        String username = getUser(ADMIN_USER);
        Assume.assumeTrue("Ignoring test no admin user configured", username != null);
        log.debug("Testing password for user " + username);
        testLoginSuccess(username);
    }

    @Test
    public void testExistingUserLogIn() {
        log.debug("Testing correct password");

        for (String username : getUsernames()) {
            testLoginSuccess(username);
            verifyUserGroups(username, getGroups(username));
        }
    }

    @Test
    public void testExistingUserWithNoEmailLogIn() {
        log.debug("Testing correct password, but no e-mail provided");
        testLoginSuccess(getUser(NO_EMAIL_USER));
    }

    @Test
    public void testDeleteSSSDFederationProvider() {
        log.debug("Testing correct password");

        String username = getUsername();
        testLoginSuccess(username);
        verifyUserGroups(username, getGroups(username));

        int componentsListSize = adminClient.realm(REALM_NAME).components().query().size();
        adminClient.realm(REALM_NAME).components().component(SSSDFederationID).remove();
        assertThat(adminClient.realm(REALM_NAME).components().query().size(), is(componentsListSize - 1));
    }


    @Test
    public void changeReadOnlyProfile() {

        String username = getUsername();

        testLoginSuccess(username);

        RealmResource realm = adminClient.realm(REALM_NAME);
        List<UserRepresentation> users = realm.users().search(username, true);
        Assert.assertEquals(1, users.size());
        UserRepresentation user = users.iterator().next();
        user.setLastName("changed");

        BadRequestException e = Assert.assertThrows(BadRequestException.class,
                () -> realm.users().get(users.iterator().next().getId()).update(user));
        ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
        Assert.assertEquals("error-user-attribute-read-only", error.getErrorMessage());
    }

    @Test
    public void changeReadOnlyPassword() {
        String username = getUsername();

        testLoginSuccess(username);

        RealmResource realm = adminClient.realm(REALM_NAME);
        List<UserRepresentation> users = realm.users().search(username, true);
        Assert.assertEquals(1, users.size());
        CredentialRepresentation newPassword = new CredentialRepresentation();
        newPassword.setType(CredentialRepresentation.PASSWORD);
        newPassword.setValue("new-password-123!");
        newPassword.setTemporary(false);

        BadRequestException e = Assert.assertThrows(BadRequestException.class,
                () -> realm.users().get(users.iterator().next().getId()).resetPassword(newPassword));
        OAuth2ErrorRepresentation error = e.getResponse().readEntity(OAuth2ErrorRepresentation.class);
        Assert.assertEquals("Can't reset password as account is read only", error.getError());
    }

    private void verifyUserGroups(String username, List<String> groups) {
        List<UserRepresentation> users = adminClient.realm(REALM_NAME).users().search(username, 0, 1);
        assertThat("There must be at least one user", users.size(), greaterThan(0));
        assertThat("Exactly our test user", users.get(0).getUsername(), is(username));
        List<GroupRepresentation> assignedGroups = adminClient.realm(REALM_NAME).users().get(users.get(0).getId()).groups();
        List<String> assignedGroupNames = assignedGroups.stream().map(GroupRepresentation::getName).collect(Collectors.toList());
        MatcherAssert.assertThat(assignedGroupNames, Matchers.hasItems(groups.toArray(new String[0])));
    }
}
