/*
 *
 *  * Copyright 2023  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.keycloak.testsuite.user.profile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.admin.ui.rest.model.UIRealmInfo;
import org.keycloak.admin.ui.rest.model.UIRealmRepresentation;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author rmartinc
 */
public class UIRealmResourceTest extends AbstractTestRealmKeycloakTest {

    private static final String TEST_PWD = "password";
    private static final String USER_WITH_VIEW_USERS_ROLE = "user-with-view-users-role";
    private static final String USER_WITHOUT_ADMIN_ROLE = "user-without-admin-role";

    private static Client httpClient;
    private static Keycloak keycloakAdminClientViewUsers;
    private static Keycloak keycloakAdminClientWithoutAdminRoles;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected AppPage appPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    @BeforeClass
    public static void initHttpClients() {
        httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true);

        keycloakAdminClientViewUsers = KeycloakBuilder.builder().serverUrl(getKeycloakServerUrl())
                .realm(TEST_REALM_NAME)
                .username(USER_WITH_VIEW_USERS_ROLE)
                .password(TEST_PWD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .resteasyClient(httpClient)
                .build();

        keycloakAdminClientWithoutAdminRoles = KeycloakBuilder.builder().serverUrl(getKeycloakServerUrl())
                .realm(TEST_REALM_NAME)
                .username(USER_WITHOUT_ADMIN_ROLE)
                .password(TEST_PWD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .resteasyClient(httpClient)
                .build();
    }

    @AfterClass
    public static void closeHttpClients() {
        if (keycloakAdminClientViewUsers != null) {
            keycloakAdminClientViewUsers.close();
        }
        if (keycloakAdminClientWithoutAdminRoles != null) {
            keycloakAdminClientWithoutAdminRoles.close();
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Override
    public void configureTestRealm(final RealmRepresentation testRealm) {
        final var userWithViewUsersRole = createTestUserRep(USER_WITH_VIEW_USERS_ROLE,
                Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_USERS);
        testRealm.getUsers().add(userWithViewUsersRole);

        final var userWithoutAdminRole = createTestUserRep(USER_WITHOUT_ADMIN_ROLE,
                Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.VIEW_GROUPS);
        testRealm.getUsers().add(userWithoutAdminRole);
    }

    @Test
    public void testNoUpdateUserProfile() throws IOException {
        RealmRepresentation rep = testRealm().toRepresentation();
        updateRealmExt(toUIRealmRepresentation(rep, null));

        assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        assertAdminEvents.assertEmpty();
    }

    @Test
    public void testSameUpdateUserProfile() throws IOException {
        RealmRepresentation rep = testRealm().toRepresentation();
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();

        updateRealmExt(toUIRealmRepresentation(rep, upConfig));
        assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        assertAdminEvents.assertEmpty();
    }

    @Test
    public void testUpdateUserProfileModification() throws IOException {
        RealmRepresentation rep = testRealm().toRepresentation();
        UPConfig upConfigOrig = testRealm().users().userProfile().getConfiguration();

        try {
            UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
            upConfig.addOrReplaceAttribute(new UPAttribute("foo",
                    new UPAttributePermissions(Set.of(), Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN))));

            updateRealmExt(toUIRealmRepresentation(rep, upConfig));
            AdminEventRepresentation adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
            Assert.assertNotNull(adminEvent.getRepresentation());
            adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
            assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

            upConfig.getAttribute("foo").setDisplayName("Foo");
            updateRealmExt(toUIRealmRepresentation(rep, upConfig));
            assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
            adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
            assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

            upConfig.getAttribute("foo").setPermissions(new UPAttributePermissions(Set.of(), Set.of(UPConfigUtils.ROLE_USER)));
            updateRealmExt(toUIRealmRepresentation(rep, upConfig));
            assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
            adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
            assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

            upConfig.getAttribute("foo").setRequired(new UPAttributeRequired(Set.of(UPConfigUtils.ROLE_ADMIN, UPConfigUtils.ROLE_USER), Set.of()));
            updateRealmExt(toUIRealmRepresentation(rep, upConfig));
            assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
            adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
            assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

            upConfig.getAttribute("foo").setValidations(Map.of("length", Map.of("min", "3", "max", "128")));
            updateRealmExt(toUIRealmRepresentation(rep, upConfig));
            assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
            adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
            assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

            updateRealmExt(toUIRealmRepresentation(rep, upConfig));
            assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
            assertAdminEvents.assertEmpty();
        } finally {
            updateRealmExt(toUIRealmRepresentation(rep, upConfigOrig));
        }
    }

    @Test
    public void testRegistrationFormWithNotReadableOrWritableRequiredEmail() throws IOException {
        RealmRepresentation testRealm = testRealm().toRepresentation();
        testRealm.setRegistrationEmailAsUsername(true);
        getCleanup().addCleanup(() -> {
            testRealm.setRegistrationEmailAsUsername(false);
            testRealm().update(testRealm);
        });
        testRealm().update(testRealm);

        // set email as not readable or writable for a user
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
        upConfig.addOrReplaceAttribute(new UPAttribute("email",
                new UPAttributePermissions(Set.of(UPConfigUtils.ROLE_ADMIN), Set.of(UPConfigUtils.ROLE_ADMIN))));
        updateRealmExt(toUIRealmRepresentation(testRealm, upConfig));

        // open the registration form
        oauth.openLoginForm();
        loginPage.form().register();
        registerPage.assertCurrent();

        Assert.assertTrue("Email is missing on the registration page.", registerPage.isEmailPresent());

        registerPage.registerWithEmailAsUsername("Tom", "Brady", "tbrady@email.com", "password", "password");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        String userId = events.expectRegister("tbrady@email.com", "tbrady@email.com").assertEvent().getUserId();
        UserRepresentation user = testRealm().users().get(userId).toRepresentation();
        assertEquals("Tom", user.getFirstName());
        assertEquals("Brady", user.getLastName());
    }

    @Test
    public void testRegistrationFormWithReadonlyUsernameAndEmail() throws IOException {
        RealmRepresentation testRealm = testRealm().toRepresentation();

        // set username and email as readonly for a user
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
        upConfig.addOrReplaceAttribute(new UPAttribute("username",
                new UPAttributePermissions(Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN), Set.of(UPConfigUtils.ROLE_ADMIN))));
        upConfig.addOrReplaceAttribute(new UPAttribute("email",
                new UPAttributePermissions(Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN), Set.of(UPConfigUtils.ROLE_ADMIN))));
        updateRealmExt(toUIRealmRepresentation(testRealm, upConfig));

        // open the registration form
        oauth.openLoginForm();
        loginPage.form().register();
        registerPage.assertCurrent();

        Assert.assertTrue("Username is missing on the registration page.", registerPage.isUsernamePresent());
        Assert.assertFalse("Email should not be present on the registration page.", registerPage.isEmailPresent());

        registerPage.register("Alice", "Wood",  null, "awood", "password", "password");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        String userId = events.expectRegister("awood", null).removeDetail("email").assertEvent().getUserId();
        UserRepresentation user = testRealm().users().get(userId).toRepresentation();
        assertEquals("awood", user.getUsername());
        assertEquals("Alice", user.getFirstName());
        assertEquals("Wood", user.getLastName());
    }

    @Test
    public void uiRealmInfoSucceedsWithAnyAdminRole() {
        final var response = getUiRealmInfo(keycloakAdminClientViewUsers.tokenManager());

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        final var responseStr = response.readEntity(String.class);
        final var uiRealmInfo = toUiRealmInfo(responseStr);
        assertNotNull(uiRealmInfo);
        assertFalse(uiRealmInfo.isUserProfileProvidersEnabled());
    }

    @Test
    public void uiRealmInfoFailsWhenNoAdminRoleIsAssigned() {
        final var response = getUiRealmInfo(keycloakAdminClientWithoutAdminRoles.tokenManager());

        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRenameRealm() throws IOException {
        RealmRepresentation rep = testRealm().toRepresentation();
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ADMIN_VIEW);
        String originalRealmName = rep.getRealm();
        String updatedName = originalRealmName + "changed";

        try {
            rep.setRealm(updatedName);
            updateRealmExt(toUIRealmRepresentation(rep, upConfig), originalRealmName);
        } finally {
            rep.setRealm(originalRealmName);
            updateRealmExt(toUIRealmRepresentation(rep, upConfig), updatedName);
            assertAdminEvents.clear();
        }
    }

    private static String getKeycloakServerUrl() {
        return getAuthServerContextRoot() + "/auth";
    }

    private static UserRepresentation createTestUserRep(final String username, final String clientId, final String roleName) {
        return UserBuilder.create().enabled(true)
                .username(username)
                .email(username + "@localhost")
                .firstName(username + "-first")
                .lastName(username + "-last")
                .password(TEST_PWD)
                .role(clientId, roleName)
                .build();
    }

    private Response getUiRealmInfo(final TokenManager tokenManager) {
        return prepareHttpRequest(TEST_REALM_NAME, "ui-ext/info", tokenManager)
                .request(MediaType.APPLICATION_JSON)
                .get();
    }

    private void updateRealmExt(UIRealmRepresentation rep) {
        updateRealmExt(rep, rep.getRealm());
    }

    private void updateRealmExt(UIRealmRepresentation rep, String realmName) {
        final var request = prepareHttpRequest(realmName, "ui-ext", adminClient.tokenManager());

        final var response = request
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(rep, MediaType.APPLICATION_JSON));
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    private WebTarget prepareHttpRequest(final String realmName, final String subPath, final TokenManager tokenManager) {
        final var realmAdminPath = "/admin/realms/" + realmName;
        return httpClient.target(getKeycloakServerUrl())
                .path(realmAdminPath + "/" + subPath)
                .register(new BearerAuthFilter(tokenManager));
    }

    private UIRealmRepresentation toUIRealmRepresentation(RealmRepresentation realm, UPConfig upConfig) throws IOException {
        UIRealmRepresentation uiRealm = deserialize(JsonSerialization.writeValueAsString(realm), UIRealmRepresentation.class);
        uiRealm.setUpConfig(upConfig);
        return uiRealm;
    }

    private UPConfig toUpConfig(final String representation) {
        return deserialize(representation, UPConfig.class);
    }

    private UIRealmInfo toUiRealmInfo(final String representation) {
        return deserialize(representation, UIRealmInfo.class);
    }

    private static <T> T deserialize(final String representation, final Class<T> type) {
        try {
            return JsonSerialization.readValue(representation, type);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
