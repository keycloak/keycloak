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

package org.keycloak.protocol.oidc;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OIDCLoginProtocolReauthenticationTest {

    private static final String ACTION_ID = "UPDATE_PASSWORD";

    @AfterEach
    public void resetTimeOffset() {
        Time.setOffset(0);
    }

    @Test
    public void testNoUserSessionDoesNotRequireReauthenticationForKcAction() {
        // e.g. SAMLIdentityProvider calls requireReauthentication(null, authSession) for kc_action=idp_link after an SSO login,
        // where there is no AUTH_TIME auth note. This must not force re-authentication at the external IdP.
        OIDCLoginProtocol protocol = createProtocol(300);
        AuthenticationSessionModel authSession = createAuthSession(ACTION_ID);

        assertFalse(protocol.requireReauthentication(null, authSession));
    }

    @Test
    public void testExpiredUserSessionRequiresReauthenticationForKcAction() {
        OIDCLoginProtocol protocol = createProtocol(300);
        AuthenticationSessionModel authSession = createAuthSession(ACTION_ID);
        UserSessionModel userSession = createUserSession(String.valueOf(Time.currentTime() - 600));

        assertTrue(protocol.requireReauthentication(userSession, authSession));
    }

    @Test
    public void testFreshUserSessionDoesNotRequireReauthenticationForKcAction() {
        OIDCLoginProtocol protocol = createProtocol(300);
        AuthenticationSessionModel authSession = createAuthSession(ACTION_ID);
        UserSessionModel userSession = createUserSession(String.valueOf(Time.currentTime() - 60));

        assertFalse(protocol.requireReauthentication(userSession, authSession));
    }

    private OIDCLoginProtocol createProtocol(int maxAuthAge) {
        RequiredActionProvider provider = (RequiredActionProvider) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{RequiredActionProvider.class},
                (proxy, method, args) -> "getMaxAuthAge".equals(method.getName()) ? maxAuthAge : null
        );
        Map<String, RequiredActionProvider> providers = Map.of(ACTION_ID, provider);
        KeycloakSession session = (KeycloakSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{KeycloakSession.class},
                (proxy, method, args) -> {
                    if ("getProvider".equals(method.getName()) && RequiredActionProvider.class.equals(args[0])) {
                        return providers.get((String) args[1]);
                    }
                    return null;
                }
        );
        return new OIDCLoginProtocol(session, null, null, null, null);
    }

    private AuthenticationSessionModel createAuthSession(String kcAction) {
        return (AuthenticationSessionModel) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{AuthenticationSessionModel.class},
                (proxy, method, args) -> {
                    if ("getClientNote".equals(method.getName()) && Constants.KC_ACTION.equals(args[0])) {
                        return kcAction;
                    }
                    return null;
                }
        );
    }

    private UserSessionModel createUserSession(String authTime) {
        return (UserSessionModel) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{UserSessionModel.class},
                (proxy, method, args) -> {
                    if ("getNote".equals(method.getName()) && AuthenticationManager.AUTH_TIME.equals(args[0])) {
                        return authTime;
                    }
                    return null;
                }
        );
    }
}
