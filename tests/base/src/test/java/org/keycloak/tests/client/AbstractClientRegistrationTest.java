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

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
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
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.client.resources.TestApplicationResource;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;

import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractClientRegistrationTest {

    protected static final String REALM_NAME = "test";
    protected static final String CLIENT_ID = "test-client";
    protected static final String CLIENT_SECRET = "test-client-secret";
    protected final Logger log = Logger.getLogger(getClass());
    protected final SuiteContextCompat suiteContext = new SuiteContextCompat();
    protected final TestingClientCompat testingClient = new TestingClientCompat();

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

    @InjectAdminClient
    Keycloak adminClient;

    ClientRegistration reg;
    private ResteasyClient testsuiteProvidersClient;

    @BeforeEach
    public void before() throws Exception {
        org.keycloak.testsuite.util.oauth.OAuthClient.updateURLs(keycloakUrls.getBase());
        oauth.realm(managedRealm.getName());
        reg = ClientRegistration.create().url(keycloakUrls.getBase(), managedRealm.getName()).build();
    }

    @AfterEach
    public void after() throws Exception {
        if (reg != null) {
            reg.close();
        }
        if (testsuiteProvidersClient != null) {
            testsuiteProvidersClient.close();
            testsuiteProvidersClient = null;
        }
    }

    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    protected ClientRepresentation buildClient() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        return client;
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

    public static class LegacyTestsuiteProvidersServerConfig extends DefaultKeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config)
                    .dependency("org.keycloak.testsuite", "integration-arquillian-testsuite-providers");
        }
    }

    public static class ClientRegistrationRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(REALM_NAME)
                    .id(REALM_NAME)
                    .loginWithEmailAllowed(true)
                    .clients(ClientBuilder.create("myclient-test")
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

    protected Cleanup getCleanup() {
        return new Cleanup(managedRealm);
    }

    protected URI getAuthServerRoot() {
        return keycloakUrls.getBaseBuilder().path("/").build();
    }

    protected RealmsResource realmsResouce() {
        return adminClient.realms();
    }

    protected String createUser(String realm, String username, String password, String... requiredActions) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(username);
        user.setEmail(username);
        user.setEmailVerified(true);
        user.setFirstName("First");
        user.setLastName("Last");

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        user.setCredentials(List.of(credential));
        user.setRequiredActions(Arrays.asList(requiredActions));
        return AdminApiUtil.createUserWithAdminClient(adminClient.realm(realm), user);
    }

    protected final class Cleanup {

        private final ManagedRealm realm;

        private Cleanup(ManagedRealm realm) {
            this.realm = realm;
        }

        public void addClientUuid(String clientUuid) {
            realm.cleanup().add(r -> {
                try {
                    r.clients().get(clientUuid).remove();
                } catch (NotFoundException ignored) {
                    // Client can already be removed by the test itself.
                }
            });
        }
    }

    protected final class SuiteContextCompat {

        public AuthServerInfoCompat getAuthServerInfo() {
            return new AuthServerInfoCompat();
        }
    }

    protected final class AuthServerInfoCompat {

        public URI getContextRoot() {
            return keycloakUrls.getBaseBuilder().build();
        }

        public URI getBrowserContextRoot() {
            return keycloakUrls.getBaseBuilder().build();
        }
    }

    protected final class TestingClientCompat {

        public TestAppCompat testApp() {
            return new TestAppCompat();
        }
    }

    protected final class TestAppCompat {

        public TestOIDCEndpointsApplicationResource oidcClientEndpoints() {
            if (testsuiteProvidersClient == null) {
                testsuiteProvidersClient = (ResteasyClient) ResteasyClientBuilder.newBuilder().build();
            }
            return testsuiteProvidersClient.target(getAuthServerRoot()).proxy(TestApplicationResource.class).oidcClientEndpoints();
        }
    }
}
