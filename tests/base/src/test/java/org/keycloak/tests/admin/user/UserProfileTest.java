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

package org.keycloak.tests.admin.user;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.representations.idm.UserProfileMetadata;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class UserProfileTest extends AbstractUserTest {

    @Test
    public void testUsernameReadOnlyIfEmailAsUsernameEnabled() {
        switchRegistrationEmailAsUsername(true);
        getCleanup().addCleanup(() -> switchRegistrationEmailAsUsername(false));
        String userId = createUser("user-metadata", "user-metadata@keycloak.org");
        UserRepresentation user = realm.users().get(userId).toRepresentation(true);
        UserProfileMetadata metadata = user.getUserProfileMetadata();
        assertNotNull(metadata);
        UserProfileAttributeMetadata username = metadata.getAttributeMetadata(UserModel.USERNAME);
        assertNotNull(username);
        assertTrue(username.isReadOnly());
        UserProfileAttributeMetadata email = metadata.getAttributeMetadata(UserModel.EMAIL);
        assertNotNull(email);
        assertFalse(email.isReadOnly());
    }

    @Test
    public void testEmailNotReadOnlyIfEmailAsUsernameEnabledAndEditUsernameDisabled() {
        switchRegistrationEmailAsUsername(true);
        getCleanup().addCleanup(() -> switchRegistrationEmailAsUsername(false));
        RealmRepresentation rep = realm.toRepresentation();
        assertFalse(rep.isEditUsernameAllowed());
        String userId = createUser("user-metadata", "user-metadata@keycloak.org");
        UserRepresentation user = realm.users().get(userId).toRepresentation(true);
        UserProfileMetadata metadata = user.getUserProfileMetadata();
        assertNotNull(metadata);
        UserProfileAttributeMetadata username = metadata.getAttributeMetadata(UserModel.USERNAME);
        assertNotNull(username);
        assertTrue(username.isReadOnly());
        UserProfileAttributeMetadata email = metadata.getAttributeMetadata(UserModel.EMAIL);
        assertNotNull(email);
        assertFalse(email.isReadOnly());
    }

    @Test
    public void testUserProfileMetadata() {
        String userId = createUser("user-metadata", "user-metadata@keycloak.org");
        UserRepresentation user = realm.users().get(userId).toRepresentation(true);
        UserProfileMetadata metadata = user.getUserProfileMetadata();
        assertNotNull(metadata);

        for (String name : managedAttributes) {
            assertNotNull(metadata.getAttributeMetadata(name));
        }
    }

    @Test
    public void defaultMaxResults() {
        UserProfileResource upResource = adminClient.realm("test").users().userProfile();
        UPConfig upConfig = upResource.getConfiguration();
        upConfig.addOrReplaceAttribute(createAttributeMetadata("aName"));
        upResource.update(upConfig);

        try {
            UsersResource users = adminClient.realms().realm("test").users();

            for (int i = 0; i < 110; i++) {
                users.create(UserBuilder.create().username("test-" + i).addAttribute("aName", "aValue").build()).close();
            }

            List<UserRepresentation> result = users.search("test", null, null);
            assertEquals(100, result.size());
            for (UserRepresentation user : result) {
                assertThat(user.getAttributes(), Matchers.notNullValue());
                assertThat(user.getAttributes().keySet(), Matchers.hasSize(1));
                assertThat(user.getAttributes(), Matchers.hasEntry(is("aName"), Matchers.contains("aValue")));
            }

            assertEquals(105, users.search("test", 0, 105).size());
            assertEquals(111, users.search("test", 0, 1000).size());
        } finally {
            upConfig.removeAttribute("aName");
            upResource.update(upConfig);
        }
    }

    @Test
    public void defaultMaxResultsBrief() {
        UserProfileResource upResource = adminClient.realm("test").users().userProfile();
        UPConfig upConfig = upResource.getConfiguration();
        upConfig.addOrReplaceAttribute(createAttributeMetadata("aName"));
        upResource.update(upConfig);

        try {
            UsersResource users = adminClient.realms().realm("test").users();

            for (int i = 0; i < 110; i++) {
                users.create(UserBuilder.create().username("test-" + i).addAttribute("aName", "aValue").build()).close();
            }

            List<UserRepresentation> result = users.search("test", null, null, true);
            assertEquals(100, result.size());
            for (UserRepresentation user : result) {
                assertThat(user.getAttributes(), Matchers.nullValue());
            }
        } finally {
            upConfig.removeAttribute("aName");
            upResource.update(upConfig);
        }
    }
}
