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

package org.keycloak.services.resources;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogoutSessionCodeChecksTest {

    /**
     * A logout authentication session has no login protocol. The "page expired" handling used for an invalid
     * session code must not try to build a login continuation URL for it (which needs the protocol and
     * threw a NullPointerException), but answer with the logout error page.
     */
    @Test
    public void showPageExpiredForProtocolLessLogoutSessionReturnsLogoutError() {
        boolean[] rollback = {false};
        KeycloakTransactionManager tx = proxy(KeycloakTransactionManager.class, (p, m, a) -> {
            if (m.getName().equals("setRollbackOnly")) {
                rollback[0] = true;
            }
            return defaultValue(m.getReturnType());
        });
        LoginFormsProvider forms = proxy(LoginFormsProvider.class, (p, m, a) -> {
            if (m.getName().equals("createErrorPage")) {
                return Response.status((Response.Status) a[0]).build();
            }
            return m.getReturnType().isInstance(p) ? p : defaultValue(m.getReturnType());
        });
        KeycloakSessionFactory factory = proxy(KeycloakSessionFactory.class, (p, m, a) -> defaultValue(m.getReturnType()));
        KeycloakSession session = proxy(KeycloakSession.class, (p, m, a) -> switch (m.getName()) {
            case "getTransactionManager" -> tx;
            case "getKeycloakSessionFactory" -> factory;
            case "getProvider" -> a[0] == LoginFormsProvider.class ? forms : null;
            default -> defaultValue(m.getReturnType());
        });
        RealmModel realm = proxy(RealmModel.class, (p, m, a) -> defaultValue(m.getReturnType()));
        AuthenticationSessionModel authSession = proxy(AuthenticationSessionModel.class, (p, m, a) -> {
            if (m.getName().equals("getProtocol")) {
                return null;
            }
            throw new AssertionError("Unexpected access to the authentication session: " + m.getName());
        });

        String[] eventError = {null};
        EventBuilder event = new EventBuilder(realm, session) {
            @Override
            public void error(String error) {
                eventError[0] = error;
            }
        }.event(EventType.LOGOUT);
        LogoutSessionCodeChecks checks = new LogoutSessionCodeChecks(realm, null, null, null, session,
                event, "invalid-code", "client", "tab");

        Response response = checks.showPageExpired(authSession);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertNull(eventError[0], "The endpoint, not this method, emits the logout error event");
        assertTrue(rollback[0], "The error page should mark the transaction for rollback");
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == Stream.class) {
            return Stream.empty();
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }
}
