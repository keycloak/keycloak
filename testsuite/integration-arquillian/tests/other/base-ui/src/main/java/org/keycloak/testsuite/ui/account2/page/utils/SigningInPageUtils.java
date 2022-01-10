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

package org.keycloak.testsuite.ui.account2.page.utils;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.SigningInPage;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;

/**
 * Helper class for SigningIn page
 */
public class SigningInPageUtils {

    public static void testModalDialog(AbstractLoggedInPage accountPage, Runnable triggerModal, Runnable onCancel) {
        triggerModal.run();
        accountPage.modal().assertIsDisplayed();
        accountPage.modal().clickCancel();
        accountPage.modal().assertIsNotDisplayed();
        onCancel.run();
        triggerModal.run();
        accountPage.modal().clickConfirm();
        accountPage.modal().assertIsNotDisplayed();
    }

    public static void assertUserCredential(String expectedUserLabel, boolean removable, SigningInPage.UserCredential userCredential) {
        assertThat(userCredential.getUserLabel(), is(expectedUserLabel));

        // we expect the credential was created/edited no longer that 2 minutes ago (1 minute might not be enough in some corner cases)
        LocalDateTime beforeNow = LocalDateTime.now().minusMinutes(2);
        LocalDateTime now = LocalDateTime.now();
        // createdAt doesn't specify seconds so it should be something like 12:47:00
        LocalDateTime createdAt = userCredential.getCreatedAt();

        assertThat("Creation time should be after time before update", createdAt.isAfter(beforeNow), is(true));
        assertThat("Creation time should be before now", createdAt.isBefore(now), is(true));

        assertThat("Remove button visible", userCredential.isRemoveBtnDisplayed(), is(removable));
        assertThat("Update button visible", userCredential.isUpdateBtnDisplayed(), is(not(removable)));
    }

    public static void testSetUpLink(RealmResource realmResource, SigningInPage.CredentialType credentialType, String requiredActionProviderId) {
        assertThat("Set up link for \"" + credentialType.getType() + "\" is not visible", credentialType.isSetUpLinkVisible(), is(true));

        RequiredActionProviderRepresentation requiredAction = new RequiredActionProviderRepresentation();
        requiredAction.setEnabled(false);
        realmResource.flows().updateRequiredAction(requiredActionProviderId, requiredAction);

        refreshPageAndWaitForLoad();

        assertThat("Set up link for \"" + credentialType.getType() + "\" is visible", credentialType.isSetUpLinkVisible(), is(false));
        assertThat("Title for \"" + credentialType.getType() + "\" is visible", credentialType.isTitleVisible(), is(false));
        assertThat("Set up link for \"" + credentialType.getType() + "\" is visible", credentialType.isNotSetUpLabelVisible(), is(false));
    }

    public static void testRemoveCredential(AbstractLoggedInPage accountPage, SigningInPage.UserCredential userCredential) {
        int countBeforeRemove = userCredential.getCredentialType().getUserCredentialsCount();

        testModalDialog(accountPage, userCredential::clickRemoveBtn, () -> {
            assertThat(userCredential.isPresent(), is(true));
            assertThat(userCredential.getCredentialType().getUserCredentialsCount(), is(countBeforeRemove));
        });
        accountPage.alert().assertSuccess();

        assertThat(userCredential.isPresent(), is(false));
        assertThat(userCredential.getCredentialType().getUserCredentialsCount(), is(countBeforeRemove - 1));
    }

    public static SigningInPage.UserCredential getNewestUserCredential(UserResource userResource, SigningInPage.CredentialType credentialType) {
        List<CredentialRepresentation> credentials = userResource.credentials();
        SigningInPage.UserCredential userCredential =
                credentialType.getUserCredential(credentials.get(credentials.size() - 1).getId());

        assertThat(userCredential.isPresent(), is(true));
        return userCredential;
    }
}
