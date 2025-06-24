/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AbstractFormAuthenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.TrustedDeviceCredentialProvider;
import org.keycloak.credential.TrustedDeviceCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.TrustedDeviceCredentialInputModel;
import org.keycloak.models.UserModel;

/**
 * @author Norbert Kelemen
 * @version $Revision: 1 $
 */
public class TrustedDeviceAuthenticator extends AbstractFormAuthenticator implements Authenticator, CredentialValidator<TrustedDeviceCredentialProvider> {


    /**
     * This method tries to parse and validate the stored Trusted Device cookie
     * If cookie does not exist or the credential is invalid (expired, deleted), then it will skip the authentication
     * and deletes the cookie (if exists).
     * @param context Authentication context
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();
        CookieProvider cookieProvider = session.getProvider(CookieProvider.class);
        CookieType cookieType = CookieType.getTrustedDeviceCookie(context.getUser().getId());
        String cookie = cookieProvider.get(cookieType);

        // No trusted device cookie exists for user, or the cookie is empty
        if (cookie == null || cookie.isEmpty()) {
            context.attempted();
            return;
        }

        // Cookie stores the credentialId and secret separated by a colon -> <credentialId>:<secret>
        var cookieParts = cookie.split(":", 2);
        if (cookieParts.length != 2) {
            // cookie's content is in invalid format
            cookieProvider.expire(cookieType);
            context.attempted();
            return;
        }

        // Validate the secret stored in the cookie
        var model = new TrustedDeviceCredentialInputModel(cookieParts[0], cookieParts[1]);
        if (context.getUser().credentialManager().isValid(model)) {
            context.success();
            return;
        }

        cookieProvider.expire(cookieType);
        context.attempted();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // no-op
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        var credentialProvider = getCredentialProvider(session);
        return user.credentialManager().isConfiguredFor(credentialProvider.getType());
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no-op
    }

    @Override
    public TrustedDeviceCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (TrustedDeviceCredentialProvider) session.getProvider(CredentialProvider.class, TrustedDeviceCredentialProviderFactory.PROVIDER_ID);
    }
}
