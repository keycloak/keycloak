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

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;

import org.junit.Assert;
import org.junit.Test;

public class AbstractRefreshTokenProviderTest {

    @Test
    public void shouldNotIncrementRefreshTokenUseCountOnRollback() throws Exception {
        AtomicInteger useCount = new AtomicInteger();
        AtomicReference<KeycloakTransaction> afterCompletion = new AtomicReference<>();

        AuthenticatedClientSessionModel clientSession = proxy(AuthenticatedClientSessionModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRefreshToken", "getLatestGeneratedRefreshToken" -> "token-id";
            case "getRefreshTokenLastRefresh" -> 0;
            case "getRefreshTokenUseCount" -> useCount.get();
            case "setRefreshTokenUseCount" -> {
                useCount.set((int) args[1]);
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });
        ClientSessionContext clientSessionContext = proxy(ClientSessionContext.class, (proxy, method, args) ->
                method.getName().equals("getClientSession") ? clientSession : defaultValue(method.getReturnType()));
        RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "isRevokeRefreshToken" -> true;
            case "getRefreshTokenMaxReuse" -> 0;
            default -> defaultValue(method.getReturnType());
        });
        KeycloakTransactionManager transactionManager = proxy(KeycloakTransactionManager.class, (proxy, method, args) -> {
            if (method.getName().equals("enlistAfterCompletion")) {
                KeycloakTransaction transaction = (KeycloakTransaction) args[0];
                transaction.begin();
                afterCompletion.set(transaction);
            }
            return defaultValue(method.getReturnType());
        });
        KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) ->
                method.getName().equals("getTransactionManager") ? transactionManager : defaultValue(method.getReturnType()));

        RefreshToken refreshToken = new RefreshToken(new AccessToken(), null, null);
        refreshToken.id("token-id");
        TokenManager.TokenValidation validation = new TokenManager.TokenValidation(null, null, clientSessionContext);

        new TestRefreshTokenProvider(session).validateTokenReuse(session, realm, refreshToken, validation, new TokenManager());

        Assert.assertEquals(0, useCount.get());
        Assert.assertNotNull(afterCompletion.get());
        afterCompletion.get().rollback();
        Assert.assertEquals(0, useCount.get());
    }

    private static Object defaultValue(Class<?> type) {
        if (type == void.class) return null;
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        throw new IllegalArgumentException("Unsupported primitive type: " + type);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler));
    }

    private static class TestRefreshTokenProvider extends DefaultRefreshTokenProvider {

        private TestRefreshTokenProvider(KeycloakSession session) {
            super(session);
        }

        private void validateTokenReuse(KeycloakSession session, RealmModel realm, RefreshToken refreshToken,
                                        TokenManager.TokenValidation validation, TokenManager tokenManager) throws Exception {
            validateTokenReuseForRefresh(session, realm, refreshToken, validation, tokenManager);
        }
    }
}
