/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.client;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractClientRegistrationTest {

    @InjectRealm(config = ClientRegistrationRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    CloseableHttpClient closeableHttpClient;

    ClientRegistration reg;

    @BeforeEach
    public void before() throws Exception {
        reg = oauth.clientRegistration();
    }

    public ClientRepresentation createClient(ClientRepresentation client) throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation response = reg.create(client);
        reg.auth(null);
        return response;
    }

    public ClientRepresentation getClient(String clientUuid) {
        try {
            return managedRealm.admin().clients().get(clientUuid).toRepresentation();
        } catch (NotFoundException e) {
            return null;
        }
    }

    void authCreateClients() {
        reg.auth(Auth.token(getToken("create-clients", "password")));
    }

    void authManageClients() {
        reg.auth(Auth.token(getToken("manage-clients", "password")));
    }

    void authNoAccess() {
        reg.auth(Auth.token(getToken("no-access", "password")));
    }

    protected String getToken(String username, String password) {
        return getToken(Constants.ADMIN_CLI_CLIENT_ID, null, username, password);
    }

    protected String getToken(String clientId, String clientSecret, String username, String password) {
        try {
            return oauth.client(clientId, clientSecret).doPasswordGrantRequest(username, password).getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class ClientRegistrationRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.clients(ClientBuilder.create("myclient-test")
                    .publicClient(true)
                    .directAccessGrantsEnabled(true));

            UserBuilder manageClientUser = UserBuilder.create()
                    .username("manage-clients")
                    .name("manage", "clients")
                    .password("password")
                    .email("manage-clients@test.com")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_CLIENTS);

            UserBuilder createClientUser = UserBuilder.create()
                    .username("create-clients")
                    .name("create", "clients")
                    .password("password")
                    .email("create-clients@test.com")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.CREATE_CLIENT);

            UserBuilder noAccessUser = UserBuilder.create()
                    .username("no-access")
                    .name("no", "access")
                    .password("password")
                    .email("no-access@test.com")
                    .emailVerified(true);

            UserBuilder appUser = UserBuilder.create()
                    .username("test-user")
                    .name("test", "user")
                    .password("password")
                    .email("test-user@localhost")
                    .emailVerified(true);

            realm.users(manageClientUser, createClientUser, noAccessUser, appUser);

            return realm;
        }
    }
}
