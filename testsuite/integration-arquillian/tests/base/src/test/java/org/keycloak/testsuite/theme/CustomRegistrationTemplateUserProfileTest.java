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

import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.forms.VerifyProfileTest.disableDynamicUserProfile;
import static org.keycloak.testsuite.forms.VerifyProfileTest.enableDynamicUserProfile;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.Profile.Feature;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

@EnableFeature(Feature.DECLARATIVE_USER_PROFILE)
public class CustomRegistrationTemplateUserProfileTest extends CustomRegistrationTemplateTest {

    private UPConfig upConfig;

    @Before
    public void onBefore() {
        upConfig = updateUserProfileConfiguration();
    }

    @After
    public void onAfter() {
        disableDynamicUserProfile(testRealm());
    }

    @Override
    @Test
    public void testRegistration() {
        upConfig.setUnmanagedAttributePolicy(null);
        testRealm().users().userProfile().update(upConfig);
        super.testRegistration();
    }

    @Test
    public void testUnmanagedAttributeEnabled() {
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        for (String name : CUSTOM_ATTRIBUTES.keySet()) {
            upConfig.removeAttribute(name);
        }
        testRealm().users().userProfile().update(upConfig);
        UserRepresentation user = register();
        assertCustomAttributes(user.getAttributes());
    }

    @Test
    public void testUnmanagedAttributeAdminEdit() {
        upConfig.setUnmanagedAttributePolicy(null);
        for (String name : CUSTOM_ATTRIBUTES.keySet()) {
            upConfig.removeAttribute(name);
        }
        testRealm().users().userProfile().update(upConfig);
        UserRepresentation user = register();
        assertNull(user.getAttributes());
    }

    @Test
    public void testUnmanagedAttributeDisabled() {
        upConfig.setUnmanagedAttributePolicy(null);
        for (String name : CUSTOM_ATTRIBUTES.keySet()) {
            upConfig.removeAttribute(name);
        }
        testRealm().users().userProfile().update(upConfig);
        UserRepresentation user = register();
        assertNull(user.getAttributes());
    }

    private UPConfig updateUserProfileConfiguration() {
        RealmRepresentation realm = testRealm().toRepresentation();
        enableDynamicUserProfile(realm);
        testRealm().update(realm);
        UPConfig upCOnfig = testRealm().users().userProfile().getConfiguration();
        upCOnfig.addOrReplaceAttribute(new UPAttribute("street", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        upCOnfig.addOrReplaceAttribute(new UPAttribute("locality", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        upCOnfig.addOrReplaceAttribute(new UPAttribute("region", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        upCOnfig.addOrReplaceAttribute(new UPAttribute("postal_code", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        upCOnfig.addOrReplaceAttribute(new UPAttribute("country", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER))));
        testRealm().users().userProfile().update(upCOnfig);
        return upCOnfig;
    }
}
