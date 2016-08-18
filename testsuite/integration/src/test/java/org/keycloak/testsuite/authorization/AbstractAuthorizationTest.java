/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.authorization;

import org.junit.Before;
import org.junit.Rule;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Invocation;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.keycloak.models.AdminRoles.ADMIN;
import static org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractAuthorizationTest {

    protected static final String TEST_REALM_NAME = "photoz";

    @Rule
    public KeycloakAuthorizationServerRule keycloak = new KeycloakAuthorizationServerRule(TEST_REALM_NAME);

    private Keycloak adminClient;

    @Before
    public void onBefore() {
        adminClient = Keycloak.getInstance(AUTH_SERVER_ROOT, MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID);
    }

    protected <R> R onAuthorizationSession(Function<AuthorizationProvider, R> function) {
        KeycloakSession keycloakSession = startKeycloakSession();
        KeycloakTransactionManager transaction = keycloakSession.getTransactionManager();

        try {
            AuthorizationProvider authorizationProvider = keycloakSession.getProvider(AuthorizationProvider.class);

            R result = function.apply(authorizationProvider);

            transaction.commit();

            return result;
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            if (keycloakSession != null) {
                keycloakSession.close();
            }
        }
    }

    protected void onAuthorizationSession(Consumer<AuthorizationProvider> consumer) {
        KeycloakSession keycloakSession = startKeycloakSession();
        KeycloakTransactionManager transaction = keycloakSession.getTransactionManager();

        try {
            AuthorizationProvider authorizationProvider = keycloakSession.getProvider(AuthorizationProvider.class);

            consumer.accept(authorizationProvider);

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            if (keycloakSession != null) {
                keycloakSession.close();
            }
        }
    }

    protected Invocation.Builder newClient(ClientModel client, String authzRelativePath) {
        return ClientBuilder.newClient()
                .register((ClientRequestFilter) requestContext -> {
                    AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
                    requestContext.getHeaders().add("Authorization", "Bearer " + accessToken.getToken());
                }).target(AUTH_SERVER_ROOT + "/admin/realms/" + TEST_REALM_NAME + "/clients/" + client.getId() + "/authz" + authzRelativePath).request();
    }

    protected ClientModel getClientByClientId(String clientId) {
        KeycloakSession session = this.keycloak.startSession();

        try {
            RealmModel realm = session.realms().getRealmByName(TEST_REALM_NAME);
            return realm.getClientByClientId(clientId);
        } finally {
            session.close();
        }
    }

    private KeycloakSession startKeycloakSession() {
        KeycloakSession keycloakSession = this.keycloak.startSession();

        keycloakSession.getContext().setRealm(keycloakSession.realms().getRealmByName(TEST_REALM_NAME));

        return keycloakSession;
    }
}
