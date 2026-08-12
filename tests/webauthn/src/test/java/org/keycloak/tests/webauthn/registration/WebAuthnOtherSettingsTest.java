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

package org.keycloak.tests.webauthn.registration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.DisabledOnWebDriver;
import org.keycloak.testframework.ui.webdriver.WebDriverSupplier;
import org.keycloak.tests.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.tests.webauthn.WebAuthnDataWrapper;

import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.COSEKeyType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@KeycloakIntegrationTest
public class WebAuthnOtherSettingsTest extends AbstractWebAuthnVirtualTest {

    public static final String CHROME_AAGUID = "01020304-0506-0708-0102-030405060708";

    @InjectRunOnServer(realmRef = "webauthn")
    RunOnServerClient runOnServer;

    @Test
    @DisabledOnWebDriver({WebDriverSupplier.FIREFOX, WebDriverSupplier.FIREFOX_HEADLESS}) // See https://github.com/keycloak/keycloak/issues/10368
    public void defaultValues() {
        registerDefaultUser("webauthn");

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());

        final String userId = userResource().toRepresentation().getId();
        assertThat(userId, notNullValue());

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION)
                .userId(userId)
                .details(Details.CUSTOM_REQUIRED_ACTION, isPasswordless()
                        ? WebAuthnPasswordlessRegisterFactory.PROVIDER_ID
                        : WebAuthnRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "webauthn")
                .details(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID);
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_CREDENTIAL)
                .userId(userId)
                .details(Details.CUSTOM_REQUIRED_ACTION, isPasswordless()
                        ? WebAuthnPasswordlessRegisterFactory.PROVIDER_ID
                        : WebAuthnRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "webauthn")
                .details(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID);

        final String credentialType = getCredentialType();
        // Soft token in Firefox does not increment counter; this test is disabled on Firefox via @DisabledOnWebDriver
        long credentialCount = 1L;

        runOnServer.run(session -> {
            final WebAuthnDataWrapper dataWrapper = new WebAuthnDataWrapper(session, USERNAME, credentialType);
            assertThat(dataWrapper, notNullValue());

            final WebAuthnCredentialData data = dataWrapper.getWebAuthnData();
            assertThat(data, notNullValue());
            assertThat(data.getCredentialId(), notNullValue());
            assertThat(data.getAaguid(), is(ALL_ZERO_AAGUID));
            assertThat(data.getAttestationStatement(), nullValue());
            assertThat(data.getCredentialPublicKey(), notNullValue());
            assertThat(data.getCounter(), is(credentialCount));
            assertThat(data.getAttestationStatementFormat(), is(AttestationConveyancePreference.NONE.getValue()));

            final COSEKey pubKey = dataWrapper.getKey();
            assertThat(pubKey, notNullValue());
            assertThat(pubKey.getAlgorithm(), notNullValue());
            assertThat(pubKey.getAlgorithm().getValue(), is(COSEAlgorithmIdentifier.ES256.getValue()));
            assertThat(pubKey.getKeyType(), is(COSEKeyType.EC2));
            assertThat(pubKey.hasPublicKey(), is(true));
        });
    }

    @Test
    public void timeout() throws Exception {
        final Integer TIMEOUT = 3; // seconds

        getVirtualAuthManager().removeAuthenticator();

        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyCreateTimeout(TIMEOUT));

        assertThat(managedRealm.admin().toRepresentation().getWebAuthnPolicyCreateTimeout(), is(TIMEOUT));

        deleteUserIfPresent(USERNAME);

        oAuthClient.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", EMAIL, USERNAME, PASSWORD);

        // User was registered. Now he needs to register WebAuthn credential
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        Thread.sleep((TIMEOUT + 2) * 1000L);

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("The Passkey operation was not allowed or timed out."));

        webAuthnErrorPage.clickTryAgain();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        webAuthnErrorPage.assertCurrent();
    }

    @Test
    public void acceptableAaguidsShouldBeEmptyOrNullByDefault() {
        assertThat(managedRealm.admin().toRepresentation().getWebAuthnPolicyAcceptableAaguids(),
                anyOf(nullValue(), Matchers.empty()));
    }

    @Test
    @DisabledOnWebDriver(WebDriverSupplier.FIREFOX) // See https://github.com/keycloak/keycloak/issues/10368
    @Disabled(
            "Requires a server-side truststore that accepts the virtual authenticator's self-signed packed " +
            "attestation. Without it, webauthn4j rejects the credential at the self-attestation check before " +
            "the AAGUID filter is reached. The old testsuite worked around this via " +
            "testingClient.testing().disableTruststoreSpi(), which is not available in the new test framework.")
    public void excludeCredentials() {
        List<String> acceptableAaguids = Collections.singletonList(ALL_ZERO_AAGUID);

        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyAcceptableAaguids(acceptableAaguids)
                .webAuthnPolicyAttestationConveyancePreference(AttestationConveyancePreference.DIRECT.getValue()));

        assertThat(managedRealm.admin().toRepresentation().getWebAuthnPolicyAcceptableAaguids(),
                Matchers.contains(ALL_ZERO_AAGUID));

        registerDefaultUser();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("This security key model is not allowed (AAGUID " + CHROME_AAGUID + "). Please use a different security key."));
    }

    @Test
    @DisabledOnWebDriver(WebDriverSupplier.FIREFOX) // See https://github.com/keycloak/keycloak/issues/10368
    @Disabled(
            "Requires a server-side truststore that accepts the virtual authenticator's self-signed packed " +
            "attestation. Without it, webauthn4j rejects the credential at the self-attestation check before " +
            "the AAGUID filter is reached. The old testsuite worked around this via " +
            "testingClient.testing().disableTruststoreSpi(), which is not available in the new test framework.")
    public void excludeCredentialsSuccess() {
        List<String> acceptableAaguids = Collections.singletonList(CHROME_AAGUID);

        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyAcceptableAaguids(acceptableAaguids)
                .webAuthnPolicyAttestationConveyancePreference(AttestationConveyancePreference.DIRECT.getValue()));

        assertThat(managedRealm.admin().toRepresentation().getWebAuthnPolicyAcceptableAaguids(),
                Matchers.contains(CHROME_AAGUID));

        registerDefaultUser("webauthn");

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
    }

    @Test
    @DisabledOnWebDriver(WebDriverSupplier.FIREFOX) // See https://github.com/keycloak/keycloak/issues/10368
    public void excludeCredentialsUsingNone() {
        List<String> acceptableAaguids = Collections.singletonList(ALL_ZERO_AAGUID);

        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyAcceptableAaguids(acceptableAaguids));

        assertThat(managedRealm.admin().toRepresentation().getWebAuthnPolicyAcceptableAaguids(),
                Matchers.contains(ALL_ZERO_AAGUID));

        registerDefaultUser();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("Your organization requires verified security keys. Attestation format 'none' is not accepted; please use a key that provides attestation."));
    }

    @Test
    @DisabledOnWebDriver(WebDriverSupplier.FIREFOX) // See https://github.com/keycloak/keycloak/issues/10368
    public void apiNotAllowedErrorMessage() throws Exception {
        final Integer TIMEOUT = 3; //seconds

        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyCreateTimeout(TIMEOUT));

        assertBrowserApiErrorMessage(options -> options.setIsUserConsenting(false),
                "The Passkey operation was not allowed or timed out.", TIMEOUT);
    }

    @Test
    @DisabledOnWebDriver(WebDriverSupplier.FIREFOX) // See https://github.com/keycloak/keycloak/issues/10368
    public void apiInvalidStateErrorMessage() {
        registerDefaultUser();
        UserRepresentation user = userResource().toRepresentation();
        logout();

        user.setRequiredActions(Collections.singletonList(isPasswordless()
                ? WebAuthnPasswordlessRegisterFactory.PROVIDER_ID
                : WebAuthnRegisterFactory.PROVIDER_ID));
        userResource().update(user);

        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(USERNAME, PASSWORD);
        loginPage.submit();

        webAuthnLoginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        webAuthnErrorPage.assertCurrent(Duration.ofSeconds(15));
        assertThat(webAuthnErrorPage.getError(), containsString("This Passkey is already registered."));
    }

    @Test
    @DisabledOnWebDriver(WebDriverSupplier.FIREFOX) // See https://github.com/keycloak/keycloak/issues/10368
    public void apiSecurityErrorMessage() {
        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyRpId("invalid.example.com"));

        deleteUserIfPresent(USERNAME);

        oAuthClient.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", EMAIL, USERNAME, PASSWORD);

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("A security error occurred during the Passkey operation. Please ensure you are on the correct site and try again."));
    }

    private void assertBrowserApiErrorMessage(Consumer<VirtualAuthenticatorOptions> optionsConsumer, String expectedMessage, Integer waitSeconds) throws Exception {
        getVirtualAuthManager().removeAuthenticator();
        VirtualAuthenticatorOptions options = getDefaultAuthenticatorOptions();
        optionsConsumer.accept(options);
        getVirtualAuthManager().useAuthenticator(options);

        deleteUserIfPresent(USERNAME);

        oAuthClient.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", EMAIL, USERNAME, PASSWORD);

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        if (waitSeconds != null) {
            Thread.sleep((waitSeconds + 2) * 1000L);
        }

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString(expectedMessage));
    }
}
