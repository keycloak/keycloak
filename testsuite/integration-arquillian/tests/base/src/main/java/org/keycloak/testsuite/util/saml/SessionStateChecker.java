package org.keycloak.testsuite.util.saml;

import org.infinispan.util.function.SerializableConsumer;
import org.infinispan.util.function.SerializableFunction;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.runonserver.FetchOnServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class SessionStateChecker implements Runnable {

    private String realmName = "demo";
    private AtomicReference<String> userSessionIdStore;
    private AtomicReference<String> expectedUserSession;
    private String expectedClientSession;
    private SerializableConsumer<UserSessionModel> consumeUserSession;
    private final Map<String, SerializableConsumer<AuthenticatedClientSessionModel>> consumeClientSession = new HashMap<>();

    private SerializableFunction<KeycloakSession, String> userSessionIdProvider;
    private SerializableFunction<KeycloakSession, String> clientSessionIdProvider;
    private final KeycloakTestingClient.Server server;


    public SessionStateChecker(KeycloakTestingClient.Server server) {
        this.server = server;
    }

    public SessionStateChecker realmName(String realmName) {
        this.realmName = realmName;
        return this;
    }

    public SessionStateChecker setUserSessionProvider(SerializableFunction<KeycloakSession, String> sessionProvider) {
        this.userSessionIdProvider = sessionProvider;
        return this;
    }

    public SessionStateChecker setClientSessionProvider(SerializableFunction<KeycloakSession, String> sessionProvider) {
        this.clientSessionIdProvider = sessionProvider;
        return this;
    }

    public SessionStateChecker storeUserSessionId(AtomicReference<String> userSessionIdStore) {
        this.userSessionIdStore = userSessionIdStore;
        return this;
    }

    public SessionStateChecker consumeClientSession(String clientSessionId, SerializableConsumer<AuthenticatedClientSessionModel> consumer) {
        consumeClientSession.merge(clientSessionId, consumer, (consumer1, consumer2) -> {
            return (SerializableConsumer<AuthenticatedClientSessionModel>) clientSessionModel -> {
                consumer1.accept(clientSessionModel);
                consumer2.accept(clientSessionModel);
            };
        });
        return this;
    }

    public SessionStateChecker consumeUserSession(SerializableConsumer<UserSessionModel> userSessionModelConsumer) {
        if (consumeUserSession == null) {
            consumeUserSession = userSessionModelConsumer;
        } else {
            consumeUserSession = mergeConsumers(consumeUserSession, userSessionModelConsumer);
        }

        return this;
    }

    public SerializableConsumer<UserSessionModel> mergeConsumers(SerializableConsumer<UserSessionModel> consumer1, SerializableConsumer<UserSessionModel> consumer2) {
        return userSessionModel -> {
            consumer1.accept(userSessionModel);
            consumer2.accept(userSessionModel);
        };
    }

    public SessionStateChecker expectedAction(String clientId, CommonClientSessionModel.Action action) {
        consumeClientSession(clientId, clientSessionModel -> {
            if (action == null) {
                assertThat(clientSessionModel, notNullValue());
                assertThat(clientSessionModel.getAction(), nullValue());
                return;
            }
            assertThat(clientSessionModel, notNullValue());
            assertThat(clientSessionModel.getAction(), equalTo(action.name()));
        });

        return this;
    }

    public SessionStateChecker expectedState(UserSessionModel.State state) {
        consumeUserSession(userSessionModel -> {
            assertThat(userSessionModel, notNullValue());
            assertThat(userSessionModel.getState(), equalTo(state));
        });

        return this;
    }

    public SessionStateChecker expectedNumberOfClientSessions(int expectedNumberOfClientSession) {
        consumeUserSession(userSession -> assertThat(userSession.getAuthenticatedClientSessions().keySet(), hasSize(expectedNumberOfClientSession)));
        return this;
    }


    public SessionStateChecker expectedUserSession(AtomicReference<String> expectedUserSession) {
        this.expectedUserSession = expectedUserSession;
        return this;
    }

    public SessionStateChecker expectedClientSession(String expectedClientSession) {
        this.expectedClientSession = expectedClientSession;
        return this;
    }

    public void run() {
        run(server, realmName, userSessionIdStore, expectedUserSession, expectedClientSession, consumeUserSession, consumeClientSession, userSessionIdProvider, clientSessionIdProvider);
    }

    public static void run(KeycloakTestingClient.Server server,
                           String realmName,
                           AtomicReference<String> userSessionIdStore,
                           AtomicReference<String> expectedUserSession,
                           String expectedClientSession,
                           SerializableConsumer<UserSessionModel> consumeUserSession,
                           Map<String, SerializableConsumer<AuthenticatedClientSessionModel>> consumeClientSession,
                           SerializableFunction<KeycloakSession, String> userSessionIdProvider,
                           SerializableFunction<KeycloakSession, String> clientSessionIdProvider) {
        if (server == null || userSessionIdProvider == null)
            throw new RuntimeException("Wrongly configured session checker");

        if (userSessionIdStore != null) {
            String userSession = server.fetchString((FetchOnServer) userSessionIdProvider::apply);
            userSessionIdStore.set(userSession.replace("\"", ""));
        }

        server.run(session -> {
            String sessionId = userSessionIdProvider.apply(session);

            if (expectedUserSession != null) {
                assertThat(sessionId, equalTo(expectedUserSession.get()));
            }

            if (expectedClientSession != null) {
                String clientSession = clientSessionIdProvider.apply(session);
                assertThat(clientSession, equalTo(expectedClientSession));
            }

            RealmModel realm = session.realms().getRealmByName(realmName);
            UserSessionModel userSessionModel = session.sessions().getUserSession(realm, sessionId);
            if (consumeUserSession != null) consumeUserSession.accept(userSessionModel);

            if (!consumeClientSession.isEmpty()) {
                consumeClientSession.forEach((id, consumer) -> consumer.accept(userSessionModel.getAuthenticatedClientSessionByClient(id)));
            }
        });


    }

}
