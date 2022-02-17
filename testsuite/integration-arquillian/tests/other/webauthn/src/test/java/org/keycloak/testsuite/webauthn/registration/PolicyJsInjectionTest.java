/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.pages.WebAuthnAuthenticatorsList;
import org.keycloak.testsuite.webauthn.updaters.AbstractWebAuthnRealmUpdater;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;
import org.keycloak.utils.StringUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verify JS attack in WebAuthn Policy
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class PolicyJsInjectionTest extends AbstractWebAuthnVirtualTest {
    // If there's an unexpected alert, the test fails -> we verified here that the script is not executed
    protected final String PROMPT_SCRIPT = "required\"; window.prompt('Injection'); \"<img id=\"image-inject\" src='none'/> ";

    // The page is redirect, if the script is executed
    protected final String REDIRECT_SCRIPT = "required\"; window.location.href = \"http://www.keycloak.org\";\"";

    @Test
    public void relyingPartyEntityName() {
        verifyInjection((updater) -> updater.setWebAuthnPolicyRpEntityName(REDIRECT_SCRIPT),
                WebAuthnRealmData::getRpEntityName,
                REDIRECT_SCRIPT);
    }

    @Test
    public void relyingPartyId() throws IOException {
        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpId(PROMPT_SCRIPT)
                .update()) {

            WebAuthnRealmData data = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(data.getRpId(), is(PROMPT_SCRIPT));

            registerDefaultUser(false);

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("The relying party ID is not a registrable domain suffix of, nor equal to the current domain."));
        }
    }

    @Test
    public void attestationConveyancePreference() {
        verifyInjection((updater) -> updater.setWebAuthnPolicyAttestationConveyancePreference(REDIRECT_SCRIPT),
                WebAuthnRealmData::getAttestationConveyancePreference,
                REDIRECT_SCRIPT,
                "Failed to read the 'attestation' property from 'PublicKeyCredentialCreationOptions': The provided value 'required\"; window.location.href = \"http://www.keycloak.org\";\"' is not a valid enum value of type AttestationConveyancePreference.");
    }

    @Test
    public void authenticatorAttachment() {
        verifyInjection((updater) -> updater.setWebAuthnPolicyAuthenticatorAttachment(REDIRECT_SCRIPT),
                WebAuthnRealmData::getAuthenticatorAttachment,
                REDIRECT_SCRIPT,
                "Failed to read the 'authenticatorAttachment' property from 'AuthenticatorSelectionCriteria': The provided value 'required\"; window.location.href = \"http://www.keycloak.org\";\"' is not a valid enum value of type AuthenticatorAttachment.");
    }

    @Test
    public void requireResidentKey() {
        // requireResidentKey is set to 'false' and the value is ignored -> success
        verifyInjection((updater) -> updater.setWebAuthnPolicyRequireResidentKey(PROMPT_SCRIPT),
                WebAuthnRealmData::getRequireResidentKey,
                PROMPT_SCRIPT);
    }

    @Test
    public void userVerificationRequirement() {
        verifyInjection((updater) -> updater.setWebAuthnPolicyUserVerificationRequirement(PROMPT_SCRIPT),
                WebAuthnRealmData::getUserVerificationRequirement,
                PROMPT_SCRIPT,
                "Failed to read the 'userVerification' property from 'AuthenticatorSelectionCriteria': The provided value 'required\"; window.prompt('Injection'); \"<img id=\"image-inject\" src='none'/> ' is not a valid enum value of type UserVerificationRequirement.");
    }

    @Test
    public void injectUserLabel() {
        final String originalLabel = "label'`;window.prompt(\"another\");'";

        registerDefaultUser(originalLabel);

        appPage.assertCurrent();

        final CredentialRepresentation credential = userResource().credentials()
                .stream()
                .filter(f -> f.getType().equals(getCredentialType()))
                .findFirst()
                .orElse(null);

        assertThat(credential, notNullValue());
        assertThat(credential.getUserLabel(), is(originalLabel));

        if (!isPasswordless()) {
            logout();

            loginPage.open();
            loginPage.assertCurrent(TEST_REALM_NAME);
            loginPage.login(USERNAME, PASSWORD);

            webAuthnLoginPage.assertCurrent();
            WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
            assertThat(authenticators, notNullValue());
            assertThat(authenticators.getItems(), not(Matchers.empty()));

            assertThat(authenticators.getLabels().get(0), is("label`;window.prompt(\"another\");"));
        }
    }

    private void verifyInjection(Consumer<AbstractWebAuthnRealmUpdater<?>> realmSetter, Function<WebAuthnRealmData, String> realmGetter, String requiredValue) {
        verifyInjection(realmSetter, realmGetter, requiredValue, "");
    }

    /**
     * Verify the possibility of executing the JS injection in WebAuthn Policy settings
     *
     * @param realmSetter   set Realm WebAuthn policy
     * @param realmGetter   get Realm WebAuthn policy
     * @param expectedValue expected value save in realm
     * @param errorMessage  expected message if it's present
     */
    private void verifyInjection(Consumer<AbstractWebAuthnRealmUpdater<?>> realmSetter, Function<WebAuthnRealmData, String> realmGetter, String expectedValue, String errorMessage) {
        AbstractWebAuthnRealmUpdater<?> updater = getWebAuthnRealmUpdater();
        realmSetter.accept(updater);

        try (Closeable u = updater.update()) {

            WebAuthnRealmData data = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmGetter.apply(data), is(expectedValue));

            boolean shouldSuccess = StringUtil.isBlank(errorMessage);

            registerDefaultUser(shouldSuccess);

            if (shouldSuccess) {
                appPage.assertCurrent();
            } else {
                webAuthnErrorPage.assertCurrent();
                assertThat(webAuthnErrorPage.getError(), containsString(errorMessage));
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot verify test scenarios for WebAuthn Policy JS Injection", e);
        }
    }
}
