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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.userprofile.config.UPConfigUtils;

/**
 * <p>Test for the User profile integration in the SSSD provider.</p>
 *
 * @author rmartinc
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SSSDUserProfileTest extends AbstractBaseSSSDTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        // enable user profile and add sssd provider in the realm
        ComponentExportRepresentation sssdComp = new ComponentExportRepresentation();
        sssdComp.setName(PROVIDER_NAME);
        sssdComp.setProviderId(PROVIDER_NAME);
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("cachePolicy", "DEFAULT");
        config.putSingle("enabled", "true");
        sssdComp.setConfig(config);

        MultivaluedHashMap<String, ComponentExportRepresentation> components = testRealm.getComponents();
        if (components == null) {
            components = new MultivaluedHashMap<>();
            testRealm.setComponents(components);
        }

        components.add(UserStorageProvider.class.getName(), sssdComp);
    }

    @Test
    public void test01LoginSuccess() throws Exception {
        // do a login to create the first sssd user in the configuration
        testLoginSuccess(getUsername());
    }

    @Test
    public void test02DefaultSSSDUserProfile() throws Exception {
        // default configuration adds all sssd attributes
        // check they are read-only in both admin and user for a SSSD user
        String username = getUsername();
        UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), username);
        UserRepresentation user = userResource.toRepresentation(true);

        // for admin the four should be read-only
        String sssdId = getSssdProviderId();
        assertUser(user, username, getEmail(username), getFirstName(username), getLastName(username), sssdId);
        assertProfileAttributes(user, null, true, UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME);
        user.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PROFILE.toString());
        userResource.update(user);

        // for user the same, the four attrs should be read-only
        oauth.doLogin(username, getPassword(username));
        WaitUtils.waitForPageToLoad();
        updateProfilePage.assertCurrent();
        Assert.assertEquals(getFirstName(username), updateProfilePage.getFirstName());
        Assert.assertEquals(getLastName(username), updateProfilePage.getLastName());
        Assert.assertEquals(getEmail(username), updateProfilePage.getEmail());
        Assert.assertFalse(updateProfilePage.getElementById(UserModel.FIRST_NAME).isEnabled());
        Assert.assertFalse(updateProfilePage.getElementById(UserModel.LAST_NAME).isEnabled());
        Assert.assertFalse(updateProfilePage.getElementById(UserModel.EMAIL).isEnabled());
        Assert.assertFalse(updateProfilePage.getElementById(UserModel.USERNAME).isEnabled());
        updateProfilePage.prepareUpdate().submit();

        // check events
        WaitUtils.waitForPageToLoad();
        appPage.assertCurrent();
        events.expectRequiredAction(EventType.UPDATE_PROFILE)
                .user(user.getId())
                .assertEvent();
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        EventRepresentation loginEvent = events.expectLogin()
                .user(Matchers.any(String.class))
                .detail(Details.USERNAME, username)
                .assertEvent();

        // logout
        AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        appPage.logout(tokenResponse.getIdToken());
        events.expectLogout(loginEvent.getSessionId()).user(loginEvent.getUserId()).assertEvent();
    }

    @Test
    public void test03DefaultInternalDBUserProfile() throws Exception {
        // check non sssd user has normal atttributes enabled
        UserResource testResource = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        UserRepresentation test = testResource.toRepresentation(true);

        // for admin the four should be editable
        assertUser(test, "test-user@localhost", "test-user@localhost", "Tom", "Brady", null);
        assertProfileAttributes(test, null, false, UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME);
        test.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PROFILE.toString());
        testResource.update(test);

        // for user the same, the four attrs should be editable
        oauth.doLogin("test-user@localhost", "password");
        WaitUtils.waitForPageToLoad();
        updateProfilePage.assertCurrent();
        Assert.assertEquals("Tom", updateProfilePage.getFirstName());
        Assert.assertEquals("Brady", updateProfilePage.getLastName());
        Assert.assertEquals("test-user@localhost", updateProfilePage.getEmail());
        Assert.assertTrue(updateProfilePage.getElementById(UserModel.FIRST_NAME).isEnabled());
        Assert.assertTrue(updateProfilePage.getElementById(UserModel.LAST_NAME).isEnabled());
        Assert.assertTrue(updateProfilePage.getElementById(UserModel.EMAIL).isEnabled());
        Assert.assertTrue(updateProfilePage.getElementById(UserModel.USERNAME).isEnabled());
        updateProfilePage.prepareUpdate().submit();

        // check events
        WaitUtils.waitForPageToLoad();
        appPage.assertCurrent();
        events.expectRequiredAction(EventType.UPDATE_PROFILE)
                .user(test.getId())
                .assertEvent();
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        EventRepresentation loginEvent = events.expectLogin()
                .user(Matchers.any(String.class))
                .detail(Details.USERNAME, "test-user@localhost")
                .assertEvent();

        // logout
        AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        appPage.logout(tokenResponse.getIdToken());
        events.expectLogout(loginEvent.getSessionId()).user(loginEvent.getUserId()).assertEvent();
    }

    @Test
    public void test04MixedSSSDUserProfile() throws Exception {
        RealmResource realm = testRealm();
        UPConfig origConfig = realm.users().userProfile().getConfiguration();
        try {
            createMixedUPConfiguration();

            // for admin all attributes are added as read-only and postal_code remains editable
            String username = getUsername();
            String sssdId = getSssdProviderId();
            UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), username);
            UserRepresentation user = userResource.toRepresentation(true);
            // first and last names are removed from the UP config (unmanaged) and are not available from the representation
            assertUser(user, username, getEmail(username), null, null, sssdId);
            assertProfileAttributes(user, null, true, UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME);
            assertProfileAttributes(user, null, false, "postal_code");

            // for user, firstName and lastName are not visible, username and email read-only, postal_code editable
            user.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PROFILE.toString());
            userResource.update(user);
            oauth.doLogin(username, getPassword(username));
            WaitUtils.waitForPageToLoad();
            updateProfilePage.assertCurrent();
            Assert.assertEquals(getEmail(username), updateProfilePage.getEmail());
            Assert.assertNull(updateProfilePage.getElementById(UserModel.FIRST_NAME));
            Assert.assertNull(updateProfilePage.getElementById(UserModel.LAST_NAME));
            Assert.assertFalse(updateProfilePage.getElementById(UserModel.EMAIL).isEnabled());
            Assert.assertFalse(updateProfilePage.getElementById(UserModel.USERNAME).isEnabled());
            Assert.assertTrue(updateProfilePage.getElementById("postal_code").isEnabled());
            updateProfilePage.prepareUpdate().otherProfileAttribute(Map.of("postal_code", "123456")).submit();
            WaitUtils.waitForPageToLoad();
            appPage.assertCurrent();

            // check events
            events.expectRequiredAction(EventType.UPDATE_PROFILE)
                    .user(user.getId())
                    .detail("updated_postal_code", "123456")
                    .assertEvent();
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
            EventRepresentation loginEvent = events.expectLogin()
                    .user(Matchers.any(String.class))
                    .detail(Details.USERNAME, username)
                    .assertEvent();

            // logout
            AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
            appPage.logout(tokenResponse.getIdToken());
            events.expectLogout(loginEvent.getSessionId()).user(loginEvent.getUserId()).assertEvent();
        } finally {
            realm.users().userProfile().update(origConfig);
        }
    }

    @Test
    public void test05MixedInternalDBUserProfile() throws Exception {
        RealmResource realm = testRealm();
        UPConfig origConfig = realm.users().userProfile().getConfiguration();
        try {
            createMixedUPConfiguration();

            // for admin firstName and lastName remains removed, the rest editable
            UserResource testResource = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
            UserRepresentation test = testResource.toRepresentation(true);
            assertUser(test, "test-user@localhost", "test-user@localhost", null, null, null);
            assertProfileAttributes(test, null, false, "username", "email", "postal_code");
            Assert.assertNull(test.getUserProfileMetadata().getAttributeMetadata(UserModel.FIRST_NAME));
            Assert.assertNull(test.getUserProfileMetadata().getAttributeMetadata(UserModel.LAST_NAME));

            // for user, firstName and lastName are not visible, username, email read-only and postal_code editable
            test.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PROFILE.toString());
            testResource.update(test);
            oauth.doLogin("test-user@localhost", "password");
            WaitUtils.waitForPageToLoad();
            updateProfilePage.assertCurrent();
            Assert.assertEquals("test-user@localhost", updateProfilePage.getEmail());
            Assert.assertNull(updateProfilePage.getElementById(UserModel.FIRST_NAME));
            Assert.assertNull(updateProfilePage.getElementById(UserModel.LAST_NAME));
            Assert.assertTrue(updateProfilePage.getElementById(UserModel.EMAIL).isEnabled());
            Assert.assertTrue(updateProfilePage.getElementById(UserModel.USERNAME).isEnabled());
            Assert.assertTrue(updateProfilePage.getElementById("postal_code").isEnabled());
            updateProfilePage.prepareUpdate().otherProfileAttribute(Map.of("postal_code", "123456")).submit();
            WaitUtils.waitForPageToLoad();
            appPage.assertCurrent();

            // check events
            events.expectRequiredAction(EventType.UPDATE_PROFILE)
                    .user(test.getId())
                    .detail("updated_postal_code", "123456")
                    .assertEvent();
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
            EventRepresentation loginEvent = events.expectLogin()
                    .user(Matchers.any(String.class))
                    .detail(Details.USERNAME, "test-user@localhost")
                    .assertEvent();

            // logout
            AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
            appPage.logout(tokenResponse.getIdToken());
            events.expectLogout(loginEvent.getSessionId()).user(loginEvent.getUserId()).assertEvent();
        } finally {
            realm.users().userProfile().update(origConfig);
        }
    }

    private void assertUser(UserRepresentation user, String expectedUsername, String expectedEmail,
            String expectedFirstName, String expectedLastname, String sssdId) {
        Assert.assertNotNull(user);
        Assert.assertEquals(expectedUsername, user.getUsername());
        Assert.assertEquals(expectedFirstName, user.getFirstName());
        Assert.assertEquals(expectedLastname, user.getLastName());
        Assert.assertEquals(expectedEmail, user.getEmail());
        Assert.assertEquals(sssdId, user.getFederationLink());
    }

    private void assertProfileAttributes(UserRepresentation user, String expectedGroup, boolean expectReadOnly, String... attributes) {
        for (String attrName : attributes) {
            UserProfileAttributeMetadata attrMetadata = user.getUserProfileMetadata().getAttributeMetadata(attrName);
            Assert.assertNotNull("Attribute " + attrName + " was not present for user " + user.getUsername(), attrMetadata);
            Assert.assertEquals("Attribute " + attrName + " for user " + user.getUsername() + ". Expected read-only: " + expectReadOnly + " but was not", expectReadOnly, attrMetadata.isReadOnly());
            Assert.assertEquals("Attribute " + attrName + " for user " + user.getUsername() + ". Expected group: " + expectedGroup + " but was " + attrMetadata.getGroup(), expectedGroup, attrMetadata.getGroup());
        }
    }

    private String getSssdProviderId() {
        List<ComponentRepresentation> comps = testRealm().components()
                .query(TEST_REALM_NAME, UserStorageProvider.class.getName(), PROVIDER_NAME);
        Assert.assertEquals(1, comps.size());
        return comps.iterator().next().getId();
    }

    private void createMixedUPConfiguration() {
        // removes firstName and lastName, adds a custom postal_code
        RealmResource realm = testRealm();
        UPConfig config = realm.users().userProfile().getConfiguration();
        config.getAttributes().remove(config.getAttribute(UserModel.FIRST_NAME));
        config.getAttributes().remove(config.getAttribute(UserModel.LAST_NAME));
        UPAttribute postalCode = new UPAttribute();
        postalCode.setName("postal_code");
        postalCode.setDisplayName("Postal Code");
        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setView(Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN));
        permissions.setEdit(Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN));
        postalCode.setPermissions(permissions);
        UPAttributeRequired required = new UPAttributeRequired();
        required.setRoles(Set.of(UPConfigUtils.ROLE_USER));
        postalCode.setRequired(required);
        config.getAttributes().add(postalCode);
        realm.users().userProfile().update(config);
    }
}
