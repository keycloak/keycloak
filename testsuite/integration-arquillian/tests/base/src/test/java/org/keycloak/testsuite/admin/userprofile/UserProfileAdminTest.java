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
import static org.keycloak.userprofile.config.UPConfigUtils.readSystemDefaultConfig;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeGroupMetadata;
import org.keycloak.representations.idm.UserProfileMetadata;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.testsuite.util.JsonTestUtils;
import org.keycloak.userprofile.config.UPConfigUtils;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserProfileAdminTest extends AbstractAdminTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void testDefaultConfigIfNoneSet() {
        JsonTestUtils.assertJsonEquals(readSystemDefaultConfig(), testRealm().users().userProfile().getConfiguration());
    }

    @Test
    public void testSetDefaultConfig() {
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig().addOrReplaceAttribute(new UPAttribute("test"));
        UserProfileResource userProfile = testRealm().users().userProfile();
        userProfile.update(config);
        getCleanup().addCleanup(() -> testRealm().users().userProfile().update(null));

        JsonTestUtils.assertJsonEquals(config, userProfile.getConfiguration());
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
    public void testUsernameRequiredAndWritableIfEmailAsUsernameDisabledAndEditUsernameAllowed() {
        RealmResource realm = testRealm();
        RealmRepresentation realmRep = realm.toRepresentation();
        Boolean registrationEmailAsUsername = realmRep.isRegistrationEmailAsUsername();
        realmRep.setRegistrationEmailAsUsername(false);
        realm.update(realmRep);
        getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });
        Boolean editUsernameAllowed = realmRep.isEditUsernameAllowed();
        realmRep.setEditUsernameAllowed(true);
        realm.update(realmRep);
        getCleanup().addCleanup(() -> {
            realmRep.setEditUsernameAllowed(editUsernameAllowed);
            realm.update(realmRep);
        });
        UserProfileResource userProfile = realm.users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        assertTrue(metadata.getAttributeMetadata(UserModel.USERNAME).isRequired());
        assertFalse(metadata.getAttributeMetadata(UserModel.USERNAME).isReadOnly());
    }

    @Test
    public void testUsernameRequiredAndWritableIfEmailAsUsernameDisabledAndEditUsernameDisabled() {
        RealmResource realm = testRealm();
        RealmRepresentation realmRep = realm.toRepresentation();
        Boolean registrationEmailAsUsername = realmRep.isRegistrationEmailAsUsername();
        realmRep.setRegistrationEmailAsUsername(false);
        realm.update(realmRep);
        getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });
        Boolean editUsernameAllowed = realmRep.isEditUsernameAllowed();
        realmRep.setEditUsernameAllowed(false);
        realm.update(realmRep);
        getCleanup().addCleanup(() -> {
            realmRep.setEditUsernameAllowed(editUsernameAllowed);
            realm.update(realmRep);
        });
        UserProfileResource userProfile = realm.users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        assertTrue(metadata.getAttributeMetadata(UserModel.USERNAME).isRequired());
        assertFalse(metadata.getAttributeMetadata(UserModel.USERNAME).isReadOnly());
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
        assertTrue(metadata.getAttributeMetadata(UserModel.USERNAME).isReadOnly());
    }

    @Test
    public void testGroupsMetadata() {
        UPConfig config = testRealm().users().userProfile().getConfiguration();

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
        userProfile.update(config);
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
            if (group.getAnnotations() == null) {
                assertEquals(group.getAnnotations(), mGroup.getAnnotations());
            } else {
                assertEquals(group.getAnnotations().size(), mGroup.getAnnotations().size());
            }
        }
        assertEquals(config.getGroups().get(0).getName(), metadata.getAttributeMetadata(UserModel.FIRST_NAME).getGroup());
    }
}
