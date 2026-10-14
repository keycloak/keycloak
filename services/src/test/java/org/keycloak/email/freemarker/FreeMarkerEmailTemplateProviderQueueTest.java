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

package org.keycloak.email.freemarker;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.RenderedEmail;
import org.keycloak.email.freemarker.EmailDeliveryQueueTest.RecordingExecutor;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeMarkerEmailTemplateProviderQueueTest {

    private static final RenderedEmail EMAIL = new RenderedEmail("subject", "text", "html");

    private final List<String> sent = new ArrayList<>();
    private final List<EmailException> failures = new ArrayList<>();
    private final List<KeycloakTransaction> afterCompletion = new ArrayList<>();
    private final RecordingExecutor executor = new RecordingExecutor();

    private EmailSenderProvider emailSender;
    private RealmModel realm;
    private UserModel user;
    private boolean realmDeleted;
    private boolean userDeleted;
    private KeycloakSession requestSession;

    @BeforeEach
    void setUp() {
        emailSender = stub(EmailSenderProvider.class, (method, args) -> {
            if ("send".equals(method)) {
                sent.add(((UserModel) args[1]).getEmail());
            }
            return null;
        });
        realm = stub(RealmModel.class, (method, args) -> switch (method) {
            case "getId" -> "test-realm";
            case "getSmtpConfig" -> Map.of();
            default -> null;
        });
        user = stub(UserModel.class, (method, args) -> switch (method) {
            case "getId" -> "user-id";
            case "getEmail" -> "user@localhost";
            case "isEnabled" -> true;
            default -> null;
        });
        KeycloakSessionFactory sessionFactory = stub(KeycloakSessionFactory.class, (method, args) -> "create".equals(method) ? backgroundSession() : null);
        KeycloakTransactionManager transactionManager = stub(KeycloakTransactionManager.class, (method, args) -> {
            if ("enlistAfterCompletion".equals(method)) {
                afterCompletion.add((KeycloakTransaction) args[0]);
            }
            return null;
        });
        requestSession = stub(KeycloakSession.class, (method, args) -> switch (method) {
            case "getKeycloakSessionFactory" -> sessionFactory;
            case "getTransactionManager" -> transactionManager;
            default -> null;
        });
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void sendsInTheBackgroundAfterCommit() {
        FreeMarkerEmailTemplateProvider provider = provider(new EmailDeliveryQueue(1, 1, executor));

        assertTrue(provider.queue(EMAIL, 0, this::failed));
        assertTrue(executor.tasks.isEmpty());

        commit(afterCompletion.get(0));
        assertTrue(sent.isEmpty());
        executor.runAll();

        assertEquals(List.of("user@localhost"), sent);
        assertTrue(failures.isEmpty());
    }

    @Test
    void sendsNothingWhenTheTransactionIsRolledBack() {
        FreeMarkerEmailTemplateProvider provider = provider(new EmailDeliveryQueue(1, 0, executor));
        assertTrue(provider.queue(EMAIL, 0, this::failed));
        assertFalse(provider.queue(EMAIL, 0, this::failed));

        rollback(afterCompletion.get(0));

        assertTrue(executor.tasks.isEmpty());
        assertEquals("The email was not sent, as the request failed", failures.get(0).getMessage());
        assertTrue(provider.queue(EMAIL, 0, this::failed));
    }

    @Test
    void doesNotSendToDeletedUsers() {
        FreeMarkerEmailTemplateProvider provider = provider(new EmailDeliveryQueue(1, 1, executor));
        userDeleted = true;

        provider.queue(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertTrue(sent.isEmpty());
        assertEquals("The user no longer exists or is disabled", failures.get(0).getMessage());
    }

    @Test
    void doesNotSendForDeletedRealms() {
        FreeMarkerEmailTemplateProvider provider = provider(new EmailDeliveryQueue(1, 1, executor));
        realmDeleted = true;

        provider.queue(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertTrue(sent.isEmpty());
        assertTrue(failures.isEmpty());
    }

    @Test
    void doesNotSendExpiredEmails() {
        FreeMarkerEmailTemplateProvider provider = provider(new EmailDeliveryQueue(1, 1, executor));

        provider.queue(EMAIL, Time.currentTime() - 1, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertTrue(sent.isEmpty());
        assertEquals("The email expired before it could be sent", failures.get(0).getMessage());
    }

    @Test
    void reportsWhenSendingFails() {
        emailSender = stub(EmailSenderProvider.class, (method, args) -> {
            throw new EmailException("Mail server unavailable");
        });
        FreeMarkerEmailTemplateProvider provider = provider(new EmailDeliveryQueue(1, 1, executor));

        provider.queue(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertEquals("Mail server unavailable", failures.get(0).getMessage());
    }

    @Test
    void reportsWhenTheEmailSenderIsDisabled() {
        emailSender = null;
        FreeMarkerEmailTemplateProvider provider = provider(new EmailDeliveryQueue(1, 1, executor));

        provider.queue(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertEquals("Email sender provider is disabled or not configured", failures.get(0).getMessage());
    }

    @Test
    void sendsNothingWhenTheServerIsShuttingDown() {
        FreeMarkerEmailTemplateProvider provider = provider(new EmailDeliveryQueue(1, 1, executor));
        executor.rejecting = true;

        provider.queue(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));

        assertTrue(sent.isEmpty());
    }

    @Test
    void subclassesSendRightAway() throws EmailException {
        FreeMarkerEmailTemplateProvider provider = new FreeMarkerEmailTemplateProvider(requestSession, new EmailDeliveryQueue(1, 1, executor)) {
        };
        provider.setRealm(realm);

        assertNull(provider.renderPasswordReset("link", 5));
        assertThrows(UnsupportedOperationException.class, () -> provider.queue(EMAIL, 0, this::failed));
    }

    @Test
    void subclassesCanSendInTheBackground() {
        FreeMarkerEmailTemplateProvider provider = new FreeMarkerEmailTemplateProvider(requestSession, new EmailDeliveryQueue(1, 1, executor)) {
            @Override
            protected boolean supportsQueuedDelivery() {
                return true;
            }
        };
        provider.setRealm(realm).setUser(user);

        assertTrue(provider.queue(EMAIL, 0, this::failed));
        commit(afterCompletion.get(0));
        executor.runAll();

        assertEquals(List.of("user@localhost"), sent);
    }

    @Test
    void providerWithoutQueueSendsRightAway() throws EmailException {
        FreeMarkerEmailTemplateProvider provider = new FreeMarkerEmailTemplateProvider(requestSession);
        provider.setRealm(realm);

        assertNull(provider.renderPasswordReset("link", 5));
    }

    private FreeMarkerEmailTemplateProvider provider(EmailDeliveryQueue queue) {
        FreeMarkerEmailTemplateProvider provider = new FreeMarkerEmailTemplateProvider(requestSession, queue);
        provider.setRealm(realm).setUser(user);
        return provider;
    }

    private void failed(KeycloakSession session, EmailException e) {
        failures.add(e);
    }

    private KeycloakSession backgroundSession() {
        return stub(KeycloakSession.class, (method, args) -> switch (method) {
            case "getProvider" -> args[0] == EmailSenderProvider.class ? emailSender : null;
            case "getTransactionManager" -> stub(KeycloakTransactionManager.class, (m, a) -> null);
            case "getContext" -> stub(KeycloakContext.class, (m, a) -> "getRealm".equals(m) ? realm : null);
            case "realms" -> stub(RealmProvider.class, (m, a) -> "getRealm".equals(m) && !realmDeleted ? realm : null);
            case "users" -> stub(UserProvider.class, (m, a) -> "getUserById".equals(m) && !userDeleted ? user : null);
            default -> null;
        });
    }

    private static void commit(KeycloakTransaction transaction) {
        transaction.begin();
        transaction.commit();
    }

    private static void rollback(KeycloakTransaction transaction) {
        transaction.begin();
        transaction.rollback();
    }

    private static <T> T stub(Class<T> type, Answer answer) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return type.getSimpleName();
                default:
                    Object result = answer.apply(method.getName(), args);
                    if (result == null && method.getReturnType() == boolean.class) {
                        return false;
                    }
                    return result;
            }
        }));
    }

    @FunctionalInterface
    private interface Answer {
        Object apply(String method, Object[] args) throws Exception;
    }
}
