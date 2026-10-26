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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.RenderedEmail;
import org.keycloak.executors.ExecutorsProvider;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeMarkerEmailTemplateProviderBackgroundDeliveryTest {

    private static final RenderedEmail EMAIL = new RenderedEmail("subject", "text", "html");

    private final List<String> sent = new ArrayList<>();
    private final List<EmailException> failures = new ArrayList<>();
    private final List<KeycloakTransaction> afterCompletion = new ArrayList<>();
    private final AtomicInteger backgroundDeliveries = new AtomicInteger();

    private RecordingExecutor executor;
    private EmailSenderProvider emailSender;
    private RealmModel realm;
    private UserModel user;
    private boolean realmDeleted;
    private boolean userDeleted;
    private KeycloakSession requestSession;

    @BeforeEach
    void setUp() {
        executor = new RecordingExecutor(2);
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
            case "getProvider" -> requestProvider((Class<?>) args[0]);
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
    void sendsInTheBackgroundAfterCommit() throws EmailException {
        provider().sendPasswordReset(EMAIL, 0, this::failed);
        assertTrue(sent.isEmpty());

        commit(afterCompletion.get(0));
        assertTrue(sent.isEmpty());
        executor.runAll();

        assertEquals(List.of("background:user@localhost"), sent);
        assertTrue(failures.isEmpty());
    }

    @Test
    void sendsRightAwayWhenAllThreadsAreBusy() throws EmailException {
        executor = new RecordingExecutor(1);
        FreeMarkerEmailTemplateProvider provider = provider();

        provider.sendPasswordReset(EMAIL, 0, this::failed);
        provider.sendPasswordReset(EMAIL, 0, this::failed);

        assertEquals(1, afterCompletion.size());
        assertEquals(List.of("request:user@localhost"), sent);
    }

    @Test
    void sendsNothingWhenTheTransactionIsRolledBack() throws EmailException {
        executor = new RecordingExecutor(1);
        FreeMarkerEmailTemplateProvider provider = provider();
        provider.sendPasswordReset(EMAIL, 0, this::failed);

        rollback(afterCompletion.get(0));

        assertTrue(executor.tasks.isEmpty());
        assertTrue(sent.isEmpty());
        // the thread is free again
        provider.sendPasswordReset(EMAIL, 0, this::failed);
        assertEquals(2, afterCompletion.size());
    }

    @Test
    void threadIsFreeAgainAfterSending() throws EmailException {
        executor = new RecordingExecutor(1);
        FreeMarkerEmailTemplateProvider provider = provider();
        provider.sendPasswordReset(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        provider.sendPasswordReset(EMAIL, 0, this::failed);

        assertEquals(2, afterCompletion.size());
    }

    @Test
    void doesNotSendExpiredEmails() throws EmailException {
        provider().sendPasswordReset(EMAIL, Time.currentTime() - 1, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertTrue(sent.isEmpty());
        assertEquals("The email expired before it could be sent", failures.get(0).getMessage());
    }

    @Test
    void doesNotSendToDeletedUsers() throws EmailException {
        userDeleted = true;

        provider().sendPasswordReset(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertTrue(sent.isEmpty());
        assertEquals("The user no longer exists or is disabled", failures.get(0).getMessage());
    }

    @Test
    void doesNotSendForDeletedRealms() throws EmailException {
        realmDeleted = true;

        provider().sendPasswordReset(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertTrue(sent.isEmpty());
        assertTrue(failures.isEmpty());
    }

    @Test
    void reportsWhenSendingFails() throws EmailException {
        emailSender = stub(EmailSenderProvider.class, (method, args) -> {
            throw new EmailException("Mail server unavailable");
        });

        provider().sendPasswordReset(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertEquals("Mail server unavailable", failures.get(0).getMessage());
    }

    @Test
    void reportsWhenTheEmailSenderIsDisabled() throws EmailException {
        emailSender = null;

        provider().sendPasswordReset(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertEquals("Email sender provider is disabled or not configured", failures.get(0).getMessage());
    }

    @Test
    void reportsWhenTheServerIsShuttingDown() throws EmailException {
        executor.rejecting = true;

        provider().sendPasswordReset(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));

        assertTrue(sent.isEmpty());
        assertEquals("The email was not sent, as the server is shutting down", failures.get(0).getMessage());
        assertEquals(0, backgroundDeliveries.get());
    }

    @Test
    void subclassesSendRightAway() throws EmailException {
        FreeMarkerEmailTemplateProvider provider = new FreeMarkerEmailTemplateProvider(requestSession, backgroundDeliveries) {
        };
        provider.setRealm(realm).setUser(user);

        assertNull(provider.renderPasswordReset("link", 5));
        assertThrows(UnsupportedOperationException.class, () -> provider.sendPasswordReset(EMAIL, 0, this::failed));
    }

    @Test
    void subclassesCanSendInTheBackground() throws EmailException {
        FreeMarkerEmailTemplateProvider provider = new FreeMarkerEmailTemplateProvider(requestSession, backgroundDeliveries) {
            @Override
            protected boolean supportsBackgroundDelivery() {
                return true;
            }
        };
        provider.setRealm(realm).setUser(user);

        provider.sendPasswordReset(EMAIL, 0, this::failed);
        commit(afterCompletion.get(0));
        executor.runAll();

        assertEquals(List.of("background:user@localhost"), sent);
    }

    @Test
    void providerWithoutFactorySendsRightAway() throws EmailException {
        FreeMarkerEmailTemplateProvider provider = new FreeMarkerEmailTemplateProvider(requestSession);
        provider.setRealm(realm).setUser(user);

        assertNull(provider.renderPasswordReset("link", 5));
    }

    private FreeMarkerEmailTemplateProvider provider() {
        FreeMarkerEmailTemplateProvider provider = new FreeMarkerEmailTemplateProvider(requestSession, backgroundDeliveries);
        provider.setRealm(realm).setUser(user);
        return provider;
    }

    private void failed(KeycloakSession session, EmailException e) {
        failures.add(e);
    }

    private Object requestProvider(Class<?> type) {
        if (type == ExecutorsProvider.class) {
            return stub(ExecutorsProvider.class, (method, args) -> "getExecutor".equals(method) ? executor : null);
        }
        if (type == EmailSenderProvider.class) {
            return tagged("request");
        }
        return null;
    }

    private KeycloakSession backgroundSession() {
        return stub(KeycloakSession.class, (method, args) -> switch (method) {
            case "getProvider" -> args[0] == EmailSenderProvider.class ? tagged("background") : null;
            case "getTransactionManager" -> stub(KeycloakTransactionManager.class, (m, a) -> null);
            case "getContext" -> stub(KeycloakContext.class, (m, a) -> "getRealm".equals(m) ? realm : null);
            case "realms" -> stub(RealmProvider.class, (m, a) -> "getRealm".equals(m) && !realmDeleted ? realm : null);
            case "users" -> stub(UserProvider.class, (m, a) -> "getUserById".equals(m) && !userDeleted ? user : null);
            default -> null;
        });
    }

    private EmailSenderProvider tagged(String session) {
        if (emailSender == null) {
            return null;
        }
        return stub(EmailSenderProvider.class, (method, args) -> {
            if ("send".equals(method)) {
                List<String> before = new ArrayList<>(sent);
                emailSender.send((Map<String, String>) args[0], (UserModel) args[1], (String) args[2], (String) args[3], (String) args[4]);
                if (sent.size() > before.size()) {
                    sent.set(sent.size() - 1, session + ":" + sent.get(sent.size() - 1));
                }
            }
            return null;
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

    /**
     * Runs the tasks only when the test says so.
     */
    private static class RecordingExecutor extends ThreadPoolExecutor {

        final List<Runnable> tasks = new ArrayList<>();
        boolean rejecting;

        RecordingExecutor(int threads) {
            super(threads, threads, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        }

        @Override
        public void execute(Runnable task) {
            if (rejecting) {
                throw new RejectedExecutionException("Shutting down");
            }
            tasks.add(task);
        }

        void runAll() {
            while (!tasks.isEmpty()) {
                tasks.remove(0).run();
            }
        }
    }
}
