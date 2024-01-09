/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.userprofile.UserProfileConstants.ROLE_USER;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.validate.validators.LengthValidator;

@EnableFeature(Profile.Feature.DECLARATIVE_USER_PROFILE)
public class AppInitiatedActionUpdateEmailUserProfileTest extends AppInitiatedActionUpdateEmailTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        VerifyProfileTest.enableDynamicUserProfile(testRealm);
    }

    @Test
    public void testCustomEmailValidator() throws Exception {
        UserProfileResource userProfile = testRealm().users().userProfile();
        UPConfig upConfig = userProfile.getConfiguration();
        UPAttribute emailConfig = upConfig.getAttribute(UserModel.EMAIL);
        emailConfig.addValidation(LengthValidator.ID, Map.of("min", "1", "max", "1"));
        getCleanup().addCleanup(() -> {
            emailConfig.getValidations().remove(LengthValidator.ID);
            userProfile.update(upConfig);
        });
        userProfile.update(upConfig);

        changeEmailUsingAIA("new@email.com");
        assertTrue(emailUpdatePage.getEmailError().contains("Length must be between 1 and 1."));

        emailConfig.getValidations().remove(LengthValidator.ID);
        userProfile.update(upConfig);
        changeEmailUsingAIA("new@email.com");
        events.expect(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, "test-user@localhost")
                .detail(Details.UPDATED_EMAIL, "new@email.com").assertEvent();
    }

    @Test
    public void testOnlyEmailSupportedInContext() throws Exception {
        UserProfileResource userProfile = testRealm().users().userProfile();
        UPConfig upConfig = userProfile.getConfiguration();
        String unexpectedAttributeName = "unexpectedAttribute";
        upConfig.addOrReplaceAttribute(new UPAttribute(unexpectedAttributeName, new UPAttributePermissions(Set.of(), Set.of(ROLE_USER)), new UPAttributeRequired(Set.of(ROLE_USER), Set.of())));
        getCleanup().addCleanup(() -> {
            upConfig.removeAttribute(unexpectedAttributeName);
            userProfile.update(upConfig);
        });
        userProfile.update(upConfig);

        assertFalse(driver.getPageSource().contains(unexpectedAttributeName));
        changeEmailUsingAIA("new@email.com");
        events.expect(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, "test-user@localhost")
                .detail(Details.UPDATED_EMAIL, "new@email.com").assertEvent();
    }
}
