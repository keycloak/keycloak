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

package org.keycloak.authentication;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;

import org.keycloak.common.util.Time;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthenticatorUtilTest {

    @AfterEach
    public void resetTimeOffset() {
        Time.setOffset(0);
    }

    @Test
    public void testNullUserSessionWithoutAuthTimeRequiresReAuth() {
        String actionId = "UPDATE_PASSWORD";
        // The user session expired or was removed while the AIA form was open and the auth session has no auth time -> must not be committed
        for (int maxAuthAge : new int[]{0, 300, -1}) {
            RequiredActionProvider provider = createProvider(maxAuthAge);
            AuthenticationSessionModel authSession = createAuthSession(actionId);
            KeycloakSession session = createSession(Collections.emptyMap());

            assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, null, authSession, provider, actionId), "maxAuthAge=" + maxAuthAge);
        }
    }

    @Test
    public void testNullUserSessionMaxAuthAgeZeroWithAuthSessionNoteReturnsFalse() {
        String actionId = "UPDATE_PASSWORD";
        RequiredActionProvider provider = createProvider(0);
        // The auth session carries the timestamp of the forced re-authentication, proving the user re-authenticated
        AuthenticationSessionModel authSession = createAuthSession(actionId, String.valueOf(Time.currentTime() - 10));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, null, authSession, provider, actionId));
    }

    @Test
    public void testNullUserSessionPositiveMaxAuthAgeExpiredReturnsTrue() {
        String actionId = "UPDATE_PASSWORD";
        RequiredActionProvider provider = createProvider(300);
        AuthenticationSessionModel authSession = createAuthSession(actionId, String.valueOf(Time.currentTime() - 600));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, null, authSession, provider, actionId));
    }

    @Test
    public void testNullAuthSessionReturnsFalse() {
        UserSessionModel userSession = createUserSession(String.valueOf(Time.currentTime()));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, null, null, "UPDATE_PASSWORD"));
    }

    @Test
    public void testNoKcActionNoteReturnsFalse() {
        AuthenticationSessionModel authSession = createAuthSession(null);
        UserSessionModel userSession = createUserSession(String.valueOf(Time.currentTime()));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, null, "UPDATE_PASSWORD"));
    }

    @Test
    public void testKcActionMismatchWithActionIdReturnsFalse() {
        AuthenticationSessionModel authSession = createAuthSession("UPDATE_PASSWORD");
        UserSessionModel userSession = createUserSession("0");
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, null, "UPDATE_EMAIL"));
    }

    @Test
    public void testProviderNotFoundInSessionReturnsFalse() {
        AuthenticationSessionModel authSession = createAuthSession("UPDATE_PASSWORD");
        UserSessionModel userSession = createUserSession("0");
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, null, "UPDATE_PASSWORD"));
    }

    @Test
    public void testAuthTimeNotExpiredReturnsFalse() {
        String actionId = "UPDATE_PASSWORD";
        int currentTime = Time.currentTime();
        int maxAuthAge = 300;
        RequiredActionProvider provider = createProvider(maxAuthAge);
        AuthenticationSessionModel authSession = createAuthSession(actionId);
        // Authenticated 1 minute ago -> valid for another 4 minutes
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 60));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));
    }

    @Test
    public void testAuthTimeExpiredReturnsTrue() {
        String actionId = "UPDATE_PASSWORD";
        int currentTime = Time.currentTime();
        int maxAuthAge = 300;
        RequiredActionProvider provider = createProvider(maxAuthAge);
        AuthenticationSessionModel authSession = createAuthSession(actionId);
        // Authenticated 10 minutes ago -> expired
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 600));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));
    }

    @Test
    public void testNullActionIdMatchesAnyKcAction() {
        String actionId = "UPDATE_PROFILE";
        int currentTime = Time.currentTime();
        int maxAuthAge = 60;
        RequiredActionProvider provider = createProvider(maxAuthAge);
        AuthenticationSessionModel authSession = createAuthSession(actionId);
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 120));
        KeycloakSession session = createSession(Map.of(actionId, provider));

        // When actionId is null, provider is resolved from session and any matching action is checked
        assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, null, null));
    }

    @Test
    public void testNullActionIdNotExpiredReturnsFalse() {
        String actionId = "UPDATE_PROFILE";
        int currentTime = Time.currentTime();
        int maxAuthAge = 300;
        RequiredActionProvider provider = createProvider(maxAuthAge);
        AuthenticationSessionModel authSession = createAuthSession(actionId);
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 30));
        KeycloakSession session = createSession(Map.of(actionId, provider));

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, null, null));
    }

    @Test
    public void testMissingAuthTimeTreatedAsZero() {
        String actionId = "UPDATE_PASSWORD";
        int maxAuthAge = 300;
        RequiredActionProvider provider = createProvider(maxAuthAge);
        AuthenticationSessionModel authSession = createAuthSession(actionId);
        // userSession has no AUTH_TIME note
        UserSessionModel userSession = createUserSession(null);
        KeycloakSession session = createSession(Collections.emptyMap());

        // 0 + 300 < currentTime -> expired
        assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));
    }

    @Test
    public void testTimeOffsetSimulatingExpiry() {
        String actionId = "UPDATE_PASSWORD";
        int currentTime = Time.currentTime();
        int maxAuthAge = 100;
        RequiredActionProvider provider = createProvider(maxAuthAge);
        AuthenticationSessionModel authSession = createAuthSession(actionId);
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime));
        KeycloakSession session = createSession(Collections.emptyMap());

        // Initially not expired
        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));

        // Advance time past maxAuthAge
        Time.setOffset(150);
        assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));
    }

    @Test
    public void testMaxAuthAgeZeroInitialEntryReturnsTrue() {
        String actionId = "UPDATE_PASSWORD";
        RequiredActionProvider provider = createProvider(0);
        // authSession has NO auth note (initial entry)
        AuthenticationSessionModel authSession = createAuthSession(actionId);
        UserSessionModel userSession = createUserSession(String.valueOf(Time.currentTime()));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));
    }

    @Test
    public void testMaxAuthAgeZeroWithAuthSessionNoteReturnsFalse() {
        String actionId = "UPDATE_PASSWORD";
        RequiredActionProvider provider = createProvider(0);
        int currentTime = Time.currentTime();
        // authSession has auth note proving the user re-authenticated -> allow the commit regardless of elapsed time
        AuthenticationSessionModel authSession = createAuthSession(actionId, String.valueOf(currentTime - 10));
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 100));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));
    }

    @Test
    public void testMaxAuthAgeZeroWithOldAuthSessionNoteStillReturnsFalse() {
        String actionId = "UPDATE_PASSWORD";
        RequiredActionProvider provider = createProvider(0);
        int currentTime = Time.currentTime();
        // Even if the auth session note is old, the user did re-authenticate in this flow -> allow the commit
        AuthenticationSessionModel authSession = createAuthSession(actionId, String.valueOf(currentTime - 200));
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 200));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));
    }

    @Test
    public void testMaxAuthAgeNegativeDefaultsToKcActionMaxAge() {
        String actionId = "UPDATE_PASSWORD";
        RequiredActionProvider provider = createProvider(-1);
        int currentTime = Time.currentTime();
        // Fallback to Constants.KC_ACTION_MAX_AGE (300). Under 300 s -> not expired.
        AuthenticationSessionModel authSession = createAuthSession(actionId, String.valueOf(currentTime - 10));
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 10));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));

        // Over 300 s -> expired
        AuthenticationSessionModel authSessionExpired = createAuthSession(actionId, String.valueOf(currentTime - 350));
        assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSessionExpired, provider, actionId));
    }

    @Test
    public void testTimestampPrecedencePrioritizesAuthSession() {
        String actionId = "UPDATE_PASSWORD";
        RequiredActionProvider provider = createProvider(300);
        int currentTime = Time.currentTime();
        // userSession is stale (authenticated 600 s ago), but authSession is fresh (authenticated 10 s ago)
        AuthenticationSessionModel authSession = createAuthSession(actionId, String.valueOf(currentTime - 10));
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 600));
        KeycloakSession session = createSession(Collections.emptyMap());

        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, actionId));
    }

    @Test
    public void testAliasDifferingFromProviderIdWithProviderIdActionIdPerformsFreshnessCheck() {
        String alias = "custom_password";
        String providerId = "UPDATE_PASSWORD";
        RequiredActionProviderModel actionModel = new RequiredActionProviderModel();
        actionModel.setAlias(alias);
        actionModel.setProviderId(providerId);

        int currentTime = Time.currentTime();
        int maxAuthAge = 300;
        RequiredActionProvider provider = createProvider(maxAuthAge);
        AuthenticationSessionModel authSession = createAuthSession(alias);
        // Authenticated 10 minutes ago -> expired
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 600));
        KeycloakSession session = createSessionWithRealm(Map.of(providerId, provider), Map.of(alias, actionModel));

        // When actionId is the provider ID ("UPDATE_PASSWORD") and kcAction is the alias ("custom_password"), the freshness check must not be skipped
        assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, providerId));

        // Authenticated 1 minute ago -> valid
        UserSessionModel freshSession = createUserSession(String.valueOf(currentTime - 60));
        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, freshSession, authSession, provider, providerId));
    }

    @Test
    public void testAliasDifferingFromProviderIdWithAliasActionIdPerformsFreshnessCheck() {
        String alias = "custom_password";
        String providerId = "UPDATE_PASSWORD";
        RequiredActionProviderModel actionModel = new RequiredActionProviderModel();
        actionModel.setAlias(alias);
        actionModel.setProviderId(providerId);

        int currentTime = Time.currentTime();
        int maxAuthAge = 300;
        RequiredActionProvider provider = createProvider(maxAuthAge);
        AuthenticationSessionModel authSession = createAuthSession(alias);
        UserSessionModel userSession = createUserSession(String.valueOf(currentTime - 600));
        KeycloakSession session = createSessionWithRealm(Map.of(providerId, provider), Map.of(alias, actionModel));

        // When actionId is the alias ("custom_password") and kcAction is the alias ("custom_password"), freshness check is performed
        assertTrue(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, alias));
    }

    @Test
    public void testAliasDifferingFromProviderIdWithUnrelatedActionIdReturnsFalse() {
        String alias = "custom_password";
        String providerId = "UPDATE_PASSWORD";
        RequiredActionProviderModel actionModel = new RequiredActionProviderModel();
        actionModel.setAlias(alias);
        actionModel.setProviderId(providerId);

        RequiredActionProvider provider = createProvider(300);
        AuthenticationSessionModel authSession = createAuthSession(alias);
        UserSessionModel userSession = createUserSession("0");
        KeycloakSession session = createSessionWithRealm(Map.of(providerId, provider), Map.of(alias, actionModel));

        // When actionId is an unrelated action ("UPDATE_EMAIL"), it must not match
        assertFalse(AuthenticatorUtil.isReAuthRequiredForKcAction(session, userSession, authSession, provider, "UPDATE_EMAIL"));
    }

    @Test
    public void testRequiredActionProviderGetMaxAuthAgeWithCustomAlias() {
        String alias = "custom_action";
        String providerId = "CUSTOM_PROVIDER";
        RequiredActionProviderModel actionModel = new RequiredActionProviderModel();
        actionModel.setAlias(alias);
        actionModel.setProviderId(providerId);

        RequiredActionConfigModel configModel = new RequiredActionConfigModel();
        configModel.setConfig(Map.of(Constants.MAX_AUTH_AGE_KEY, "45"));

        AuthenticationSessionModel authSession = createAuthSession(alias);
        KeycloakSession session = createSessionWithContext(authSession, Map.of(alias, actionModel), Map.of(alias, configModel));

        RequiredActionProvider defaultProvider = new RequiredActionProvider() {
            @Override
            public void evaluateTriggers(RequiredActionContext context) {}

            @Override
            public void requiredActionChallenge(RequiredActionContext context) {}

            @Override
            public void processAction(RequiredActionContext context) {}

            @Override
            public void close() {}
        };

        assertEquals(45, defaultProvider.getMaxAuthAge(session));
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

    private AuthenticationSessionModel createAuthSession(String kcAction) {
        return createAuthSession(kcAction, null);
    }

    private AuthenticationSessionModel createAuthSession(String kcAction, String authTime) {
        return (AuthenticationSessionModel) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{AuthenticationSessionModel.class},
                (proxy, method, args) -> {
                    if ("getClientNote".equals(method.getName()) && Constants.KC_ACTION.equals(args[0])) {
                        return kcAction;
                    }
                    if ("getAuthNote".equals(method.getName()) && AuthenticationManager.AUTH_TIME.equals(args[0])) {
                        return authTime;
                    }
                    return null;
                }
        );
    }

    private RequiredActionProvider createProvider(int maxAuthAge) {
        return (RequiredActionProvider) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{RequiredActionProvider.class},
                (proxy, method, args) -> {
                    if ("getMaxAuthAge".equals(method.getName())) {
                        return maxAuthAge;
                    }
                    return null;
                }
        );
    }

    private KeycloakSession createSession(Map<String, RequiredActionProvider> providers) {
        return createSessionWithRealm(providers, Collections.emptyMap());
    }

    private KeycloakSession createSessionWithRealm(Map<String, RequiredActionProvider> providers, Map<String, RequiredActionProviderModel> realmActions) {
        RealmModel realm = (RealmModel) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{RealmModel.class},
                (proxy, method, args) -> {
                    if ("getRequiredActionProviderByAlias".equals(method.getName())) {
                        return realmActions.get((String) args[0]);
                    }
                    if ("getRequiredActionProvidersStream".equals(method.getName())) {
                        return realmActions.values().stream();
                    }
                    return null;
                }
        );
        KeycloakContext context = (KeycloakContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{KeycloakContext.class},
                (proxy, method, args) -> {
                    if ("getRealm".equals(method.getName())) {
                        return realm;
                    }
                    return null;
                }
        );
        return (KeycloakSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{KeycloakSession.class},
                (proxy, method, args) -> {
                    if ("getContext".equals(method.getName())) {
                        return context;
                    }
                    if ("getProvider".equals(method.getName()) && RequiredActionProvider.class.equals(args[0])) {
                        String id = (String) args[1];
                        return providers != null ? providers.get(id) : null;
                    }
                    return null;
                }
        );
    }

    private KeycloakSession createSessionWithContext(AuthenticationSessionModel authSession, Map<String, RequiredActionProviderModel> realmActions, Map<String, RequiredActionConfigModel> realmConfigs) {
        RealmModel realm = (RealmModel) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{RealmModel.class},
                (proxy, method, args) -> {
                    if ("getRequiredActionProviderByAlias".equals(method.getName())) {
                        return realmActions.get((String) args[0]);
                    }
                    if ("getRequiredActionConfigByAlias".equals(method.getName())) {
                        return realmConfigs.get((String) args[0]);
                    }
                    if ("getRequiredActionProvidersStream".equals(method.getName())) {
                        return realmActions.values().stream();
                    }
                    return null;
                }
        );
        KeycloakContext context = (KeycloakContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{KeycloakContext.class},
                (proxy, method, args) -> {
                    if ("getRealm".equals(method.getName())) {
                        return realm;
                    }
                    if ("getAuthenticationSession".equals(method.getName())) {
                        return authSession;
                    }
                    return null;
                }
        );
        return (KeycloakSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{KeycloakSession.class},
                (proxy, method, args) -> {
                    if ("getContext".equals(method.getName())) {
                        return context;
                    }
                    return null;
                }
        );
    }
}
