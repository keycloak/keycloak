/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.util.Base64Url;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.WebAuthnRealmData;
import org.keycloak.testframework.ui.webdriver.BrowserType;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.webauthn.AbstractWebAuthnVirtualTest;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.COSEKeyType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class WebAuthnOtherSettingsTest extends AbstractWebAuthnVirtualTest {

    private static final String CHROME_AAGUID = "01020304-0506-0708-0102-030405060708";

    @Test
    public void defaultValues() {
        registerDefaultUser("webauthn");

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());

        final String userId = userResource().toRepresentation().getId();
        Assertions.assertNotNull(userId);

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION)
                .userId(userId)
                .details(Details.CUSTOM_REQUIRED_ACTION, registerProviderId())
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "webauthn")
                .details(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID);
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_CREDENTIAL)
                .userId(userId)
                .details(Details.CUSTOM_REQUIRED_ACTION, registerProviderId())
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "webauthn")
                .details(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID);

        final String credentialType = getCredentialType();
        // Soft token in Firefox does not increment counter
        final long credentialCount = driver.getBrowserType() == BrowserType.FIREFOX ? 0L : 1L;

        CredentialData data = runOnServer.fetch(session -> {
            UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
            CredentialModel credential = user.credentialManager()
                    .getStoredCredentialsByTypeStream(credentialType)
                    .findFirst()
                    .orElseThrow();
            WebAuthnCredentialData webAuthnData = WebAuthnCredentialModel.createFromCredentialModel(credential).getWebAuthnCredentialData();

            ObjectConverter converter = new ObjectConverter();
            COSEKey pubKey = converter.getCborConverter().readValue(Base64Url.decode(webAuthnData.getCredentialPublicKey()), COSEKey.class);

            CredentialData result = new CredentialData();
            result.hasCredentialId = webAuthnData.getCredentialId() != null;
            result.aaguid = webAuthnData.getAaguid();
            result.hasAttestationStatement = webAuthnData.getAttestationStatement() != null;
            result.hasCredentialPublicKey = webAuthnData.getCredentialPublicKey() != null;
            result.counter = webAuthnData.getCounter();
            result.attestationStatementFormat = webAuthnData.getAttestationStatementFormat();
            result.coseAlgorithm = pubKey.getAlgorithm().getValue();
            result.coseKeyType = pubKey.getKeyType().name();
            result.coseHasPublicKey = pubKey.hasPublicKey();
            return result;
        }, CredentialData.class);

        Assertions.assertTrue(data.hasCredentialId);
        Assertions.assertEquals(ALL_ZERO_AAGUID, data.aaguid);
        Assertions.assertFalse(data.hasAttestationStatement);
        Assertions.assertTrue(data.hasCredentialPublicKey);
        Assertions.assertEquals(credentialCount, data.counter);
        Assertions.assertEquals(AttestationConveyancePreference.NONE.getValue(), data.attestationStatementFormat);
        Assertions.assertEquals((long) COSEAlgorithmIdentifier.ES256.getValue(), data.coseAlgorithm);
        Assertions.assertEquals(COSEKeyType.EC2.name(), data.coseKeyType);
        Assertions.assertTrue(data.coseHasPublicKey);
    }

    @Test
    public void timeout() {
        final int timeout = 3; // seconds

        getVirtualAuthManager().removeAuthenticator();
        managedRealm.updateWithCleanup(r1 -> r1.webAuthn(isPasswordless(), wAuhN -> wAuhN.timeout(timeout)));

        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        Assertions.assertEquals(timeout, isPasswordless() ? realmRep.getWebAuthnPolicyPasswordlessCreateTimeout() : realmRep.getWebAuthnPolicyCreateTimeout());

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", EMAIL, USERNAME, PASSWORD);
        webAuthnRegisterPage.assertCurrent();
        String userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), USERNAME).getId();
        managedRealm.cleanup().add(r -> r.users().get(userId).remove());

        webAuthnRegisterPage.clickRegister();


        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("The Passkey operation was not allowed or timed out."));

        webAuthnErrorPage.clickTryAgain();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        webAuthnErrorPage.assertCurrent();
    }

    @Test
    public void acceptableAaguidsShouldBeEmptyOrNullByDefault() {
        WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
        assertThat(realmData.getAcceptableAaguids(), anyOf(nullValue(), Matchers.empty()));
    }

    @Test
    public void excludeCredentials() {
        managedRealm.updateWithCleanup(
                r -> r.webAuthn(isPasswordless(), wAuhN -> wAuhN
                        .acceptableAaguids(List.of(ALL_ZERO_AAGUID))
                        .attestationConveyancePreference(AttestationConveyancePreference.DIRECT.getValue())
                )
        );

        disableTruststoreSpi();
        try {
            WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
            Assertions.assertEquals(List.of(ALL_ZERO_AAGUID), realmData.getAcceptableAaguids());

            registerDefaultUser();

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("This security key model is not allowed (AAGUID " + CHROME_AAGUID + "). Please use a different security key."));
        } finally {
            reenableTruststoreSpi();
        }
    }

    @Test
    public void excludeCredentialsSuccess() {
        managedRealm.updateWithCleanup(
                r -> r.webAuthn(isPasswordless(), wAuhN -> wAuhN
                        .acceptableAaguids(List.of(CHROME_AAGUID))
                        .attestationConveyancePreference(AttestationConveyancePreference.DIRECT.getValue())
                )
        );

        disableTruststoreSpi();
        try {
            WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
            Assertions.assertEquals(List.of(CHROME_AAGUID), realmData.getAcceptableAaguids());

            registerDefaultUser();

            Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        } finally {
            reenableTruststoreSpi();
        }
    }

    @Test
    public void excludeCredentialsUsingNone() {
        // Acceptable AAGUIDs restricted, but attestation left at the default (none): registration must be rejected
        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), wAuthN -> wAuthN.acceptableAaguids(List.of(ALL_ZERO_AAGUID))));

        WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
        Assertions.assertEquals(List.of(ALL_ZERO_AAGUID), realmData.getAcceptableAaguids());

        registerDefaultUser();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("Your organization requires verified security keys. Attestation format 'none' is not accepted; please use a key that provides attestation."));
    }

    @Test
    public void apiNotAllowedErrorMessage() {
        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), wAuhN -> wAuhN.timeout(3)));
        assertBrowserApiErrorMessage(options -> options.setIsUserConsenting(false),
                "The Passkey operation was not allowed or timed out.");
    }

    @Test
    public void apiInvalidStateErrorMessage() {
        registerDefaultUser();
        UserRepresentation user = userResource().toRepresentation();
        logout();

        user.setRequiredActions(List.of(registerProviderId()));
        userResource().update(user);

        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(USERNAME, PASSWORD);
        loginPage.submit();

        webAuthnLoginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("This Passkey is already registered."));
    }

    @Test
    public void apiSecurityErrorMessage() {
        managedRealm.updateWithCleanup(r1 -> r1.webAuthn(isPasswordless(), wAuhN -> wAuhN.rpId("invalid.example.com")));

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", EMAIL, USERNAME, PASSWORD);
        webAuthnRegisterPage.assertCurrent();
        String userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), USERNAME).getId();
        managedRealm.cleanup().add(r -> r.users().get(userId).remove());

        webAuthnRegisterPage.clickRegister();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("A security error occurred during the Passkey operation. Please ensure you are on the correct site and try again."));
    }

    private void assertBrowserApiErrorMessage(Consumer<VirtualAuthenticatorOptions> optionsConsumer, String expectedMessage) {
        getVirtualAuthManager().removeAuthenticator();
        VirtualAuthenticatorOptions options = getDefaultAuthenticatorOptions();
        optionsConsumer.accept(options);
        getVirtualAuthManager().useAuthenticator(options);

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", EMAIL, USERNAME, PASSWORD);
        webAuthnRegisterPage.assertCurrent();
        String userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), USERNAME).getId();
        managedRealm.cleanup().add(r -> r.users().get(userId).remove());

        webAuthnRegisterPage.clickRegister();


        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString(expectedMessage));
    }

    private String registerProviderId() {
        return isPasswordless() ? WebAuthnPasswordlessRegisterFactory.PROVIDER_ID : WebAuthnRegisterFactory.PROVIDER_ID;
    }

    public static class CredentialData implements Serializable {
        public boolean hasCredentialId;
        public String aaguid;
        public boolean hasAttestationStatement;
        public boolean hasCredentialPublicKey;
        public long counter;
        public String attestationStatementFormat;
        public long coseAlgorithm;
        public String coseKeyType;
        public boolean coseHasPublicKey;
    }
}
