/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.admin.userprofile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.REALM_USER_PROFILE_ENABLED;
import static org.keycloak.userprofile.config.UPConfigUtils.readDefaultConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.common.Profile;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeGroupMetadata;
import org.keycloak.representations.idm.UserProfileMetadata;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.userprofile.config.UPAttribute;
import org.keycloak.userprofile.config.UPConfig;
import org.keycloak.userprofile.config.UPGroup;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE)
public class UserProfileAdminTest extends AbstractAdminTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getAttributes() == null) {
            testRealm.setAttributes(new HashMap<>());
        }
        testRealm.getAttributes().put(REALM_USER_PROFILE_ENABLED, Boolean.TRUE.toString());
    }

    @Test
    public void testDefaultConfigIfNoneSet() {
        assertEquals(readDefaultConfig(), testRealm().users().userProfile().getConfiguration());
    }

    @Test
    public void testSetDefaultConfig() {
        String rawConfig = "{\"attributes\": [{\"name\": \"test\"}]}";
        UserProfileResource userProfile = testRealm().users().userProfile();
        userProfile.update(rawConfig);
        getCleanup().addCleanup(() -> testRealm().users().userProfile().update(null));

        assertEquals(rawConfig, userProfile.getConfiguration());
    }

    @Test
    public void testEmailRequiredIfEmailAsUsernameEnabled() {
        RealmResource realm = testRealm();
        RealmRepresentation realmRep = realm.toRepresentation();
        Boolean registrationEmailAsUsername = realmRep.isRegistrationEmailAsUsername();
        realmRep.setRegistrationEmailAsUsername(true);
        realm.update(realmRep);
        getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });
        UserProfileResource userProfile = realm.users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        assertTrue(metadata.getAttributeMetadata(UserModel.EMAIL).isRequired());
    }

    @Test
    public void testEmailNotRequiredIfEmailAsUsernameDisabled() {
        RealmResource realm = testRealm();
        RealmRepresentation realmRep = realm.toRepresentation();
        Boolean registrationEmailAsUsername = realmRep.isRegistrationEmailAsUsername();
        realmRep.setRegistrationEmailAsUsername(false);
        realm.update(realmRep);
        getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });
        UserProfileResource userProfile = realm.users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        assertFalse(metadata.getAttributeMetadata(UserModel.EMAIL).isRequired());
    }

    @Test
    public void testUsernameRequiredIfEmailAsUsernameDisabled() {
        RealmResource realm = testRealm();
        RealmRepresentation realmRep = realm.toRepresentation();
        Boolean registrationEmailAsUsername = realmRep.isRegistrationEmailAsUsername();
        realmRep.setRegistrationEmailAsUsername(false);
        realm.update(realmRep);
        getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });
        UserProfileResource userProfile = realm.users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        assertTrue(metadata.getAttributeMetadata(UserModel.USERNAME).isRequired());
    }

    @Test
    public void testUsernameNotRequiredIfEmailAsUsernameEnabled() {
        RealmResource realm = testRealm();
        RealmRepresentation realmRep = realm.toRepresentation();
        Boolean registrationEmailAsUsername = realmRep.isRegistrationEmailAsUsername();
        realmRep.setRegistrationEmailAsUsername(true);
        realm.update(realmRep);
        getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });
        UserProfileResource userProfile = realm.users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        assertFalse(metadata.getAttributeMetadata(UserModel.USERNAME).isRequired());
    }

    @Test
    public void testGroupsMetadata() throws IOException {
        UPConfig config = JsonSerialization.readValue(testRealm().users().userProfile().getConfiguration(), UPConfig.class);

        for (int i = 0; i < 3; i++) {
            UPGroup group = new UPGroup();
            group.setName("name-" + i);
            group.setDisplayHeader("displayHeader-" + i);
            group.setDisplayDescription("displayDescription-" + i);
            group.setAnnotations(Map.of("k1", "v1", "k2", "v2", "k3", "v3"));
            config.addGroup(group);
        }

        UPAttribute firstName = config.getAttribute(UserModel.FIRST_NAME);
        firstName.setGroup(config.getGroups().get(0).getName());
        UserProfileResource userProfile = testRealm().users().userProfile();
        userProfile.update(JsonSerialization.writeValueAsString(config));
        getCleanup().addCleanup(() -> testRealm().users().userProfile().update(null));

        UserProfileMetadata metadata = testRealm().users().userProfile().getMetadata();
        List<UserProfileAttributeGroupMetadata> groups = metadata.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertEquals(config.getGroups().size(), groups.size());
        for (UPGroup group : config.getGroups()) {
            UserProfileAttributeGroupMetadata mGroup = metadata.getAttributeGroupMetadata(group.getName());
            assertNotNull(mGroup);
            assertEquals(group.getName(), mGroup.getName());
            assertEquals(group.getDisplayHeader(), mGroup.getDisplayHeader());
            assertEquals(group.getDisplayDescription(), mGroup.getDisplayDescription());
            assertEquals(group.getAnnotations().size(), mGroup.getAnnotations().size());
        }
        assertEquals(config.getGroups().get(0).getName(), metadata.getAttributeMetadata(UserModel.FIRST_NAME).getGroup());
    }
}
