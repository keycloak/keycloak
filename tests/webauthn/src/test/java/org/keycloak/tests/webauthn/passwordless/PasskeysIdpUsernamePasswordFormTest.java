/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.webauthn.passwordless;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.IdpConfirmLinkPage;
import org.keycloak.tests.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Passkeys on the re-authentication page of the first broker login flow ({@code IdpUsernamePasswordForm}).
 */
public class PasskeysIdpUsernamePasswordFormTest extends AbstractWebAuthnVirtualTest {

    private static final String IDP_ALIAS = "provider-idp";
    private static final String IDP_CLIENT_ID = "broker-client";
    private static final String IDP_CLIENT_SECRET = "broker-secret";

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectPage
    IdpConfirmLinkPage idpConfirmLinkPage;

    @Override
    public boolean isPasswordless() {
        return true;
    }

    @Test
    public void passkeyErrorKeepsUsernameAndHidesRegistration() {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // short timeout so the test doesn't wait long for the passkey sign-in to fail
        managedRealm.updateWithCleanup(r -> r.webAuthn(true, builder ->
                builder.rpEntityName("localhost")
                        .residentKey(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                        .userVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                        .passkeysEnabled(true)
                        .timeout(3)
        ));

        // existing user with a passkey, the realm allows registration
        registerDefaultUser();
        UserRepresentation user = userResource().toRepresentation();
        logout();

        managedRealm.admin().identityProviders().create(IdentityProviderBuilder.create()
                .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                .alias(IDP_ALIAS)
                .attribute("clientId", IDP_CLIENT_ID)
                .attribute("clientSecret", IDP_CLIENT_SECRET)
                .attribute("authorizationUrl", providerRealm.getBaseUrl() + "/protocol/openid-connect/auth")
                .attribute("tokenUrl", providerRealm.getBaseUrl() + "/protocol/openid-connect/token")
                .build()).close();
        managedRealm.cleanup().add(r -> r.identityProviders().get(IDP_ALIAS).remove());

        // the same user logs in through the identity provider and adds it to the existing account
        oAuthClient.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(USERNAME, PASSWORD);
        loginPage.submit();
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();

        // no SMTP server is configured, so the existing account is verified by re-authentication
        loginPage.assertCurrent();
        Assertions.assertEquals(user.getUsername(), loginPage.getUsername());
        Assertions.assertFalse(loginPage.isRegisterLinkPresent());

        // remove the authenticator so the passkey sign-in fails with NotAllowedError, as when the user cancels it
        getVirtualAuthManager().removeAuthenticator();
        webAuthnLoginPage.clickAuthenticate();
        driver.waiting().until(d -> loginPage.getErrorMessage().isPresent());
        loginPage.assertCurrent();
        MatcherAssert.assertThat(loginPage.getErrorMessage().orElse(null), Matchers.containsString("The Passkey operation was not allowed or timed out."));
        Assertions.assertEquals(user.getUsername(), loginPage.getUsername());
        Assertions.assertFalse(loginPage.isRegisterLinkPresent());

        // the account is still linked by re-authentication with the password
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();
        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
    }

    public static class ProviderRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(ClientBuilder.create(IDP_CLIENT_ID)
                            .secret(IDP_CLIENT_SECRET)
                            .redirectUris("*"))
                    .users(UserBuilder.create(USERNAME)
                            .name("firstName", "lastName")
                            .email(EMAIL)
                            .emailVerified(true)
                            .password(PASSWORD));
        }
    }
}
