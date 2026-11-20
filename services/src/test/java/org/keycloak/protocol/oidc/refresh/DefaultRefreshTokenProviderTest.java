package org.keycloak.protocol.oidc.refresh;

import java.lang.reflect.Proxy;

import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class DefaultRefreshTokenProviderTest {

    @AfterEach
    public void resetTime() {
        Time.setOffset(0);
    }

    @Test
    public void offlineRefreshTokenIsNotIssuedBeforeOfflineSessionStart() {
        int[] offlineSessionStart = new int[1];
        ClientModel client = stub(ClientModel.class);
        UserSessionModel userSession = stub(UserSessionModel.class);
        AuthenticatedClientSessionModel clientSession = stub(AuthenticatedClientSessionModel.class);
        ClientSessionContext clientSessionCtx = stub(ClientSessionContext.class);

        TokenManager.AccessTokenResponseBuilder responseBuilder = new TokenManager.AccessTokenResponseBuilder(
                null, client, null, null, userSession, clientSessionCtx, new TokenManager()) {
            @Override
            public void createOrUpdateOfflineSession() {
                // Simulates slow database calls: the offline sessions record their start time two seconds later.
                Time.setOffset(Time.getOffset() + 2);
                offlineSessionStart[0] = Time.currentTime();
            }
        };
        responseBuilder.accessToken(new AccessToken());

        DefaultRefreshTokenProvider provider = new DefaultRefreshTokenProvider(null);
        RefreshToken refreshToken = provider.generateRefreshToken(
                new InitialRefreshTokenContext(clientSessionCtx, responseBuilder, null, true, null));

        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, refreshToken.getType());
        // The same check TokenManager.validateToken applies to the offline user session and client session start
        // on the first refresh. The simulated delay is longer than its one-second tolerance.
        assertFalse(refreshToken.isIssuedBeforeSessionStart(offlineSessionStart[0]),
                "Offline refresh token must not be rejected on its first use as issued before its session start");
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            Class<?> returnType = method.getReturnType();
            if (returnType == ClientModel.class && method.getName().equals("getClient")) {
                return stub(ClientModel.class);
            }
            if (returnType == UserSessionModel.class) {
                return stub(UserSessionModel.class);
            }
            if (returnType == AuthenticatedClientSessionModel.class) {
                return stub(AuthenticatedClientSessionModel.class);
            }
            if (returnType == RealmModel.class) {
                return stub(RealmModel.class);
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == boolean.class) {
                return false;
            }
            return null;
        });
    }
}
