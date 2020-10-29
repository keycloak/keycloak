/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.WebAuthnUsernamelessRegisterFactory;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.WebAuthnUsernamelessCredentialProvider;
import org.keycloak.credential.WebAuthnUsernamelessCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Collections;
import java.util.List;

/**
 * Authenticator for WebAuthn authentication with usernameless credential. This class is temporary and will be likely
 * removed in the future during future improvements in authentication SPI
 */
public class WebAuthnUsernamelessAuthenticator extends WebAuthnAuthenticator {

    public WebAuthnUsernamelessAuthenticator(KeycloakSession session) { super(session); }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    protected WebAuthnPolicy getWebAuthnPolicy(AuthenticationFlowContext context) {
        return context.getRealm().getWebAuthnPolicyUsernameless();
    }

    @Override
    protected String getCredentialType() {
        return WebAuthnCredentialModel.TYPE_USERNAMELESS;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // ask the user to do required action to register webauthn authenticator
        AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();
        if (!authenticationSession.getRequiredActions().contains(WebAuthnUsernamelessRegisterFactory.PROVIDER_ID)) {
            authenticationSession.addRequiredAction(WebAuthnUsernamelessRegisterFactory.PROVIDER_ID);
        }
    }

    @Override
    public List<RequiredActionFactory> getRequiredActions(KeycloakSession session) {
        return Collections.singletonList((WebAuthnUsernamelessRegisterFactory)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, WebAuthnUsernamelessRegisterFactory.PROVIDER_ID));
    }


    @Override
    public WebAuthnUsernamelessCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (WebAuthnUsernamelessCredentialProvider)session.getProvider(CredentialProvider.class, WebAuthnUsernamelessCredentialProviderFactory.PROVIDER_ID);
    }

}
