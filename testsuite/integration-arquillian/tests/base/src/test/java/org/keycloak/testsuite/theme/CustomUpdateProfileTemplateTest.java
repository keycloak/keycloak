/*
 *  Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.theme;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.UserBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CustomUpdateProfileTemplateTest extends AbstractTestRealmKeycloakTest {

    static final Map<String, String> CUSTOM_ATTRIBUTES = Map.of("street", "street",
            "locality",
            "locality",
            "region", "region",
            "postal_code", "postal_code",
            "country", "country");

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginUpdateProfilePage updateProfilePage;

    @Page
    protected AppPage appPage;

    private UPConfig upConfig;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setLoginTheme("address");
        // the custom theme expects email as username and the username field is not rendered at all
        testRealm.setRegistrationEmailAsUsername(true);
    }

    @Before
    public void onBefore() {
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("tom")
                .email("tom@keycloak.org")
                .password("password")
                .firstName("Tom")
                .lastName("Brady")
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name())
                .build();
        Response resp = testRealm().users().create(user);
        String userId = ApiUtil.getCreatedId(resp);
        resp.close();
        getCleanup().addUserId(userId);

        upConfig = updateUserProfileConfiguration();
    }

    @Test
    public void testUpdateProfile() {
        UserRepresentation user = getUser("tom");
        Map<String, List<String>> attributes = user.getAttributes();
        assertNull(attributes);
        user = updateProfile();
        assertCustomAttributes(user.getAttributes());
    }

    @Test
    public void testUnmanagedAttributeEnabled() {
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        for (String name : CUSTOM_ATTRIBUTES.keySet()) {
            upConfig.removeAttribute(name);
        }
        testRealm().users().userProfile().update(upConfig);
        testUpdateProfile();
    }

    @Test
    public void testUnmanagedAttributeAdminEdit() {
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ADMIN_EDIT);
        for (String name : CUSTOM_ATTRIBUTES.keySet()) {
            upConfig.removeAttribute(name);
        }
        testRealm().users().userProfile().update(upConfig);
        UserRepresentation user = updateProfile();
        assertNull(user.getAttributes());
    }

    @Test
    public void testUnmanagedAttributeDisabled() {
        for (String name : CUSTOM_ATTRIBUTES.keySet()) {
            upConfig.removeAttribute(name);
        }
        testRealm().users().userProfile().update(upConfig);
        UserRepresentation user = updateProfile();
        assertNull(user.getAttributes());
    }

    protected UserRepresentation updateProfile() {
        navigateToUpdateProfilePage();
        updateProfilePage.update(CUSTOM_ATTRIBUTES.entrySet().stream()
                .map((Function<Entry<String, String>, Entry<String, String>>) entry -> new SimpleEntry<>(Constants.USER_ATTRIBUTES_PREFIX + entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
        return getUser("tom");
    }

    protected static void assertCustomAttributes(Map<String, List<String>> attributes) {
        assertNotNull(attributes);
        for (Entry<String, String> attribute : CUSTOM_ATTRIBUTES.entrySet()) {
            String name = attribute.getKey();
            List<String> values = attributes.get(name);
            assertNotNull(values);
            assertFalse(values.isEmpty());
            assertEquals(CUSTOM_ATTRIBUTES.get(name), values.get(0));
        }
    }

    protected UserRepresentation getUser(String username) {
        List<UserRepresentation> users = testRealm().users().search(username);
        assertFalse(users.isEmpty());
        return testRealm().users().get(users.get(0).getId()).toRepresentation();
    }

    private void navigateToUpdateProfilePage() {
        loginPage.open();
        loginPage.login("tom@keycloak.org", "password");
        updateProfilePage.assertCurrent();
    }

    private UPConfig updateUserProfileConfiguration() {
        UPConfig upCOnfig = testRealm().users().userProfile().getConfiguration();
        upCOnfig.setUnmanagedAttributePolicy(null);
        upCOnfig.addOrReplaceAttribute(new UPAttribute("street", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        upCOnfig.addOrReplaceAttribute(new UPAttribute("locality", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        upCOnfig.addOrReplaceAttribute(new UPAttribute("region", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        upCOnfig.addOrReplaceAttribute(new UPAttribute("postal_code", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        upCOnfig.addOrReplaceAttribute(new UPAttribute("country", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        testRealm().users().userProfile().update(upCOnfig);
        return upCOnfig;
    }
}
