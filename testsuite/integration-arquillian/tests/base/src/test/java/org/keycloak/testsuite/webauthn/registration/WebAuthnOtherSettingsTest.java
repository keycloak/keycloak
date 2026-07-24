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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.utils.WebAuthnDataWrapper;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;

import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.COSEKeyType;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverFirefox;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnOtherSettingsTest extends AbstractWebAuthnVirtualTest {


    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void defaultValues() {
        registerDefaultUser("webauthn");

        WaitUtils.waitForPageToLoad();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        final String userId = Optional.ofNullable(userResource().toRepresentation())
                .map(UserRepresentation::getId)
                .orElse(null);

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
        // Soft token in Firefox does not increment counter
        long credentialCount = isDriverFirefox(driver) ? 0 : 1L;

        getTestingClient().server(TEST_REALM_NAME).run(session -> {
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
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void timeout() throws IOException {
        final Integer TIMEOUT = 3; //seconds

        getVirtualAuthManager().removeAuthenticator();

        try (Closeable u = getWebAuthnRealmUpdater().setWebAuthnPolicyCreateTimeout(TIMEOUT).update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
            assertThat(realmData.getCreateTimeout(), is(TIMEOUT));

            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", EMAIL, USERNAME, generatePassword(USERNAME));

            // User was registered. Now he needs to register WebAuthn credential
            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();

            pause((TIMEOUT + 2) * 1000);

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("The Passkey operation was not allowed or timed out."));

            webAuthnErrorPage.clickTryAgain();
            waitForPageToLoad();

            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();

            webAuthnErrorPage.assertCurrent();
        }
    }

    @Test
    public void acceptableAaguidsShouldBeEmptyOrNullByDefault() {
        WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
        assertThat(realmData.getAcceptableAaguids(), anyOf(nullValue(), Matchers.empty()));
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void excludeCredentials() throws IOException {
        List<String> acceptableAaguids = Collections.singletonList(ALL_ZERO_AAGUID);

        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyAcceptableAaguids(acceptableAaguids)
                .setWebAuthnPolicyAttestationConveyancePreference(AttestationConveyancePreference.DIRECT.getValue())
                .update()) {
            // webauthn virtual emulator in chrome sets a self signed certificate every time, truststore needs to be disabled
            testingClient.testing().disableTruststoreSpi();

            WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
            assertThat(realmData.getAcceptableAaguids(), Matchers.contains(ALL_ZERO_AAGUID));

            registerDefaultUser();

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("This security key model is not allowed (AAGUID " + CHROME_AAGUID + "). Please use a different security key."));
        } finally {
            testingClient.testing().reenableTruststoreSpi();
        }
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void excludeCredentialsSuccess() throws IOException {
        List<String> acceptableAaguids = Collections.singletonList(CHROME_AAGUID);

        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyAcceptableAaguids(acceptableAaguids)
                .setWebAuthnPolicyAttestationConveyancePreference(AttestationConveyancePreference.DIRECT.getValue())
                .update()) {
            // webauthn virtual emulator in chrome sets a self signed certificate every time, truststore needs to be disabled
            testingClient.testing().disableTruststoreSpi();

            WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
            assertThat(realmData.getAcceptableAaguids(), Matchers.contains(CHROME_AAGUID));

            registerDefaultUser();

            Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        } finally {
            testingClient.testing().reenableTruststoreSpi();
        }
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void excludeCredentialsUsingNone() throws IOException {
        List<String> acceptableAaguids = Collections.singletonList(ALL_ZERO_AAGUID);

        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyAcceptableAaguids(acceptableAaguids)
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
            assertThat(realmData.getAcceptableAaguids(), Matchers.contains(ALL_ZERO_AAGUID));

            registerDefaultUser();

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("Your organization requires verified security keys. Attestation format 'none' is not accepted; please use a key that provides attestation."));
        }
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void apiNotAllowedErrorMessage() throws IOException {
        final Integer TIMEOUT = 3; //seconds
        try (Closeable u = getWebAuthnRealmUpdater().setWebAuthnPolicyCreateTimeout(TIMEOUT).update()) {
            assertBrowserApiErrorMessage(options -> options.setIsUserConsenting(false),
                    "The Passkey operation was not allowed or timed out.", TIMEOUT);
        }
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void apiInvalidStateErrorMessage() throws IOException {
        registerDefaultUser();
        UserRepresentation user = userResource().toRepresentation();
        logout();

        user.setRequiredActions(Collections.singletonList(isPasswordless()
                ? WebAuthnPasswordlessRegisterFactory.PROVIDER_ID
                : WebAuthnRegisterFactory.PROVIDER_ID));
        userResource().update(user);

        oauth.openLoginForm();
        waitForPageToLoad();
        loginPage.assertCurrent();
        loginPage.login(USERNAME, getPassword(USERNAME));

        waitForPageToLoad();
        webAuthnLoginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();

        waitForPageToLoad();
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("This Passkey is already registered."));
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void apiSecurityErrorMessage() throws IOException {
        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpId("invalid.example.com")
                .update()) {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();
            registerPage.register("firstName", "lastName", EMAIL, USERNAME, generatePassword(USERNAME));

            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("A security error occurred during the Passkey operation. Please ensure you are on the correct site and try again."));
        }
    }

    private void assertBrowserApiErrorMessage(Consumer<VirtualAuthenticatorOptions> optionsConsumer, String expectedMessage, Integer waitSeconds) throws IOException {
        getVirtualAuthManager().removeAuthenticator();
        VirtualAuthenticatorOptions options = getDefaultAuthenticatorOptions();
        optionsConsumer.accept(options);
        getVirtualAuthManager().useAuthenticator(options);

        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", EMAIL, USERNAME, generatePassword(USERNAME));

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        if (waitSeconds != null) {
            pause((waitSeconds + 2) * 1000);
        }

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString(expectedMessage));
    }
}
