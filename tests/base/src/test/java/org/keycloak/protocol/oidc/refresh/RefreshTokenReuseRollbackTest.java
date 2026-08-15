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
package org.keycloak.protocol.oidc.refresh;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.RefreshToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.tests.oauth.RefreshTokenTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class RefreshTokenReuseRollbackTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectRealm(config = RefreshTokenTest.RefreshTokenTestRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = TestRealmUserConfig.class)
    ManagedUser user;

    @InjectRunOnServer(permittedPackages = "org.keycloak.protocol.oidc.refresh")
    RunOnServerClient runOnServer;

    @Test
    public void rollbackRestoresRefreshTokenReuseCount() {
        realm.updateWithCleanup(r -> r.revokeRefreshToken(true).refreshTokenMaxReuse(1));

        oauth.doLogin(user.getUsername(), "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertEquals(200, response.getStatusCode());

        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        String realmName = realm.getName();
        String userSessionId = refreshToken.getSessionId();
        String reuseKey = new TokenManager().getReuseIdKey(refreshToken);

        runOnServer.run(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            assertEquals(0, clientSession.getRefreshTokenUseCount(reuseKey));

            Object tokenReuseRollback = enlistTokenReuseRollback(session);
            incrementTokenReuseCount(tokenReuseRollback, clientSession, reuseKey);
            incrementTokenReuseCount(tokenReuseRollback, clientSession, reuseKey);
            assertEquals(2, clientSession.getRefreshTokenUseCount(reuseKey));
            session.getTransactionManager().setRollbackOnly();
        });

        Integer countAfterRollback = runOnServer.fetch(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            return clientSession.getRefreshTokenUseCount(reuseKey);
        }, Integer.class);

        assertEquals(0, countAfterRollback,
                "A rolled-back refresh must not consume token reuse allowance");
    }

    private static Object enlistTokenReuseRollback(Object session) {
        try {
            Class<?> rollbackType = Arrays.stream(AbstractRefreshTokenProvider.class.getDeclaredClasses())
                    .filter(type -> type.getSimpleName().equals("TokenReuseRollback"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Unable to find refresh-token reuse rollback type"));
            var method = AbstractRefreshTokenProvider.class.getDeclaredMethod("enlistTokenReuseRollback",
                    org.keycloak.models.KeycloakSession.class,
                    rollbackType);
            method.setAccessible(true);
            var constructor = method.getParameterTypes()[1].getDeclaredConstructor();
            constructor.setAccessible(true);
            Object tokenReuseRollback = constructor.newInstance();
            method.invoke(null, session, tokenReuseRollback);
            return tokenReuseRollback;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to enlist refresh-token reuse rollback", e);
        }
    }

    private static void incrementTokenReuseCount(Object tokenReuseRollback, Object clientSession, String reuseKey) {
        try {
            var method = AbstractRefreshTokenProvider.class.getDeclaredMethod("incrementTokenReuseCount",
                    tokenReuseRollback.getClass(),
                    org.keycloak.models.AuthenticatedClientSessionModel.class,
                    String.class);
            method.setAccessible(true);
            method.invoke(null, tokenReuseRollback, clientSession, reuseKey);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError("Unable to invoke refresh-token reuse increment", e);
        }
    }
}
