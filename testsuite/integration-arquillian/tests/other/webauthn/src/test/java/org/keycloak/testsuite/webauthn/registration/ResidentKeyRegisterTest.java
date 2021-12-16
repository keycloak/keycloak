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

package org.keycloak.testsuite.webauthn.registration;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.webauthn.utils.PropertyRequirement;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;
import org.openqa.selenium.virtualauthenticator.Credential;

import java.io.Closeable;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class ResidentKeyRegisterTest extends AbstractWebAuthnRegisterTest{

    @Test
    public void residentKeyNotRequiredNoRK() {
        assertResidentKey(true, PropertyRequirement.NO, false);
    }

    @Test
    public void residentKeyNotRequiredPresent() {
        assertResidentKey(true, PropertyRequirement.NO, true);
    }

    @Ignore("Not working")
    @Test
    public void residentKeyRequiredCorrect() {
        assertResidentKey(true, PropertyRequirement.YES, true);
    }

    @Test
    public void residentKeyRequiredWrong() {
        assertResidentKey(false, PropertyRequirement.YES, false);
    }

    private void assertResidentKey(boolean shouldSuccess, PropertyRequirement requirement, boolean hasResidentKey) {
        Credential credential;
        getVirtualAuthManager().useAuthenticator(getDefaultAuthenticatorOptions().setHasResidentKey(hasResidentKey));

        if (hasResidentKey) {
            credential = getDefaultResidentKeyCredential();
        } else {
            credential = getDefaultNonResidentKeyCredential();
        }

        getVirtualAuthManager().getCurrent().getAuthenticator().addCredential(credential);

        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRequireResidentKey(requirement.getValue())
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getRequireResidentKey(), containsString(requirement.getValue()));

            registerDefaultWebAuthnUser(shouldSuccess);

            displayErrorMessageIfPresent();

            assertThat(webAuthnErrorPage.isCurrent(), is(!shouldSuccess));
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
