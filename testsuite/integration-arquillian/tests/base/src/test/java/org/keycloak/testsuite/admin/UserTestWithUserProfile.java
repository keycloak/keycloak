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

package org.keycloak.testsuite.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.representations.idm.UserProfileMetadata;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.userprofile.config.UPAttribute;
import org.keycloak.userprofile.config.UPAttributePermissions;
import org.keycloak.userprofile.config.UPConfig;
import org.keycloak.util.JsonSerialization;

@EnableFeature(Feature.DECLARATIVE_USER_PROFILE)
public class UserTestWithUserProfile extends UserTest {

    @Before
    public void onBefore() throws IOException {
        RealmRepresentation realmRep = realm.toRepresentation();
        VerifyProfileTest.disableDynamicUserProfile(realm);
        assertAdminEvents.poll();
        realm.update(realmRep);
        assertAdminEvents.poll();
        VerifyProfileTest.enableDynamicUserProfile(realmRep);
        realm.update(realmRep);
        assertAdminEvents.poll();
        VerifyProfileTest.setUserProfileConfiguration(realm, null);
        UPConfig upConfig = JsonSerialization.readValue(realm.users().userProfile().getConfiguration(), UPConfig.class);

        for (String name : managedAttributes) {
            upConfig.addAttribute(createAttributeMetadata(name));
        }

        VerifyProfileTest.setUserProfileConfiguration(realm, JsonSerialization.writeValueAsString(upConfig));
    }

    @Test
    public void testUserProfileMetadata() {
        String userId = createUser("user-metadata", "user-metadata@keycloak.org");
        UserRepresentation user = realm.users().get(userId).toRepresentation(true);
        UserProfileMetadata metadata = user.getUserProfileMetadata();
        assertNotNull(metadata);

        for (String name : managedAttributes) {
            assertNotNull(getAttributeMetadata(metadata, name));
        }
    }

    @Test
    public void testUsernameReadOnlyIfEmailAsUsernameEnabled() {
        switchRegistrationEmailAsUsername(true);
        getCleanup().addCleanup(() -> switchRegistrationEmailAsUsername(false));
        String userId = createUser("user-metadata", "user-metadata@keycloak.org");
        UserRepresentation user = realm.users().get(userId).toRepresentation(true);
        UserProfileMetadata metadata = user.getUserProfileMetadata();
        assertNotNull(metadata);
        UserProfileAttributeMetadata username = getAttributeMetadata(metadata, UserModel.USERNAME);
        assertNotNull(username);
        assertTrue(username.isReadOnly());
        UserProfileAttributeMetadata email = getAttributeMetadata(metadata, UserModel.EMAIL);
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
        UserProfileAttributeMetadata username = getAttributeMetadata(metadata, UserModel.USERNAME);
        assertNotNull(username);
        assertTrue(username.isReadOnly());
        UserProfileAttributeMetadata email = getAttributeMetadata(metadata, UserModel.EMAIL);
        assertNotNull(email);
        assertFalse(email.isReadOnly());
    }

    @Nullable
    private static UserProfileAttributeMetadata getAttributeMetadata(UserProfileMetadata metadata, String name) {
        UserProfileAttributeMetadata attrMetadata = null;

        for (UserProfileAttributeMetadata m : metadata.getAttributes()) {
            if (name.equals(m.getName())) {
                attrMetadata = m;
            }
        }
        return attrMetadata;
    }

    private UPAttribute createAttributeMetadata(String name) {
        UPAttribute attribute = new UPAttribute();
        attribute.setName(name);
        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(Set.of("user", "admin"));
        attribute.setPermissions(permissions);
        this.managedAttributes.add(name);
        return attribute;
    }

    @Override
    protected boolean isDeclarativeUserProfile() {
        return true;
    }
}
