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

import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.utils.WebAuthnDataWrapper;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;

import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.COSEKeyType;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxDriver;

import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverFirefox;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import static org.hamcrest.CoreMatchers.allOf;
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

    @Page
    protected AppPage appPage;

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void defaultValues() {
        registerDefaultUser("webauthn");

        WaitUtils.waitForPageToLoad();
        appPage.assertCurrent();

        final String userId = Optional.ofNullable(userResource().toRepresentation())
                .map(UserRepresentation::getId)
                .orElse(null);

        assertThat(userId, notNullValue());

        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, isPasswordless()
                        ? WebAuthnPasswordlessRegisterFactory.PROVIDER_ID
                        : WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "webauthn")
                .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID)
                .assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, isPasswordless()
                        ? WebAuthnPasswordlessRegisterFactory.PROVIDER_ID
                        : WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "webauthn")
                .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID)
                .assertEvent();

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

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getCreateTimeout(), is(TIMEOUT));

            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", EMAIL, USERNAME, generatePassword(USERNAME));

            // User was registered. Now he needs to register WebAuthn credential
            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();

            pause((TIMEOUT + 2) * 1000);

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("The operation either timed out or was not allowed"));

            webAuthnErrorPage.clickTryAgain();
            waitForPageToLoad();

            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();

            assertThat(webAuthnErrorPage.isCurrent(), is(false));
        }
    }

    @Test
    public void acceptableAaguidsShouldBeEmptyOrNullByDefault() {
        WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
        assertThat(realmData.getAcceptableAaguids(), anyOf(nullValue(), Matchers.empty()));
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void excludeCredentials() throws IOException {
        List<String> acceptableAaguids = Collections.singletonList(ALL_ONE_AAGUID);

        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyAcceptableAaguids(acceptableAaguids)
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getAcceptableAaguids(), Matchers.contains(ALL_ONE_AAGUID));

            registerDefaultUser();

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), allOf(containsString("not acceptable aaguid"), containsString(ALL_ZERO_AAGUID)));
        }
    }
}
