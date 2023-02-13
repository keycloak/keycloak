/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.test;

import static org.keycloak.test.builders.ClientBuilder.AccessType.PUBLIC;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.builders.ClientBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A helper class that makes creating tests a bit easier.
 *
 * <p>
 *    Usage example:
 *    <pre>{@code
 *    new FluentTestsHelper()
 *        .init()
 *        .createDirectGrantClient("direct-grant-client")
 *        .deleteClient("direct-grant-client")
 *        .createTestUser("seb", "seb")
 *        .assignRoleWithUser("seb", "user")
 *        .deleteTestUser("seb")
 *        .deleteRole("user");
 *    }</pre>
 * </p>
 */
public class FluentTestsHelper implements Closeable {

    protected static class ClientData {
        private final ClientRepresentation clientRepresentation;
        private final String registrationCode;

        public ClientData(ClientRepresentation clientRepresentation, String registrationCode) {
            this.clientRepresentation = clientRepresentation;
            this.registrationCode = registrationCode;
        }

        public ClientRepresentation getClientRepresentation() {
            return clientRepresentation;
        }

        public String getRegistrationCode() {
            return registrationCode;
        }
    }

    public static final String DEFAULT_KEYCLOAK_URL = "http://localhost:8080/auth";
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin";
    public static final String DEFAULT_ADMIN_REALM = "master";
    public static final String DEFAULT_ADMIN_CLIENT = "admin-cli";
    public static final String DEFAULT_TEST_REALM = DEFAULT_ADMIN_REALM;
    public static final String DEFAULT_USER_ROLE = "user";

    protected final String keycloakBaseUrl;
    protected final String adminUserName;
    protected final String adminPassword;
    protected final String adminClient;
    protected final String adminRealm;

    protected String testRealm;
    protected Keycloak keycloak;
    protected String accessToken;
    protected volatile boolean isInitialized;
    protected Map<String, ClientData> createdClients = new HashMap<>();

    /**
     * Creates a new helper instance.
     */
    public FluentTestsHelper() {
        this(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    /**
     * Creates a new helper instance.
     *
     * @param adminUserName Admin username.
     * @param adminPassword Admin password.
     */
    public FluentTestsHelper(String adminUserName, String adminPassword) {
        this(DEFAULT_KEYCLOAK_URL, adminUserName, adminPassword, DEFAULT_ADMIN_REALM, DEFAULT_ADMIN_CLIENT, DEFAULT_TEST_REALM);
    }

    /**
     * Creates a new helper instance.
     *
     * @param keycloakBaseUrl Full keycloak URL.
     * @param adminUserName Admin username.
     * @param adminPassword Admin password.
     * @param adminRealm Master realm name.
     * @param adminClient Admin Client name.
     * @param testRealm new instance.
     */
    public FluentTestsHelper(String keycloakBaseUrl, String adminUserName, String adminPassword, String adminRealm, String adminClient, String testRealm) {
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.testRealm = testRealm;
        this.adminUserName = adminUserName;
        this.adminPassword = adminPassword;
        this.adminRealm = adminRealm;
        this.adminClient = adminClient;
    }

    /**
     * Initialization method.
     *
     * @return <code>this</code>
     */
    public FluentTestsHelper init() {
        keycloak = createKeycloakInstance(keycloakBaseUrl, adminRealm, adminUserName, adminPassword, adminClient);
        accessToken = generateInitialAccessToken();
        isInitialized = true;
        return this;
    }

    /**
     * @return Returns <code>true</code> if this helper has been initialized.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    protected Keycloak createKeycloakInstance(String keycloakBaseUrl, String realm, String username, String password, String clientId) {
        return Keycloak.getInstance(keycloakBaseUrl, realm, username, password, clientId);
    }

    /**
     * For more complex test scenarios
     *
     * @return Keycloak Client instance
     */
    public Keycloak getKeycloakInstance() {
        assert isInitialized;
        return keycloak;
    }

    protected String generateInitialAccessToken() {
        ClientInitialAccessCreatePresentation rep = new ClientInitialAccessCreatePresentation();
        rep.setCount(2);
        rep.setExpiration(180);
        ClientInitialAccessPresentation initialAccess = keycloak.realms().realm(testRealm).clientInitialAccess().create(rep);
        return initialAccess.getToken();
    }

    /**
     * Creates a new client based on its representation.
     *
     * @param clientRepresentation Client data.
     * @return <code>this</code>
     */
    public FluentTestsHelper createClient(ClientRepresentation clientRepresentation) throws ClientRegistrationException, JsonProcessingException {
        assert isInitialized;
        ClientRegistration reg = ClientRegistration.create()
                .url(keycloakBaseUrl, testRealm)
                .build();
        reg.auth(Auth.token(accessToken));
        clientRepresentation = reg.create(clientRepresentation);
        String registrationAccessCode = clientRepresentation.getRegistrationAccessToken();
        reg.auth(Auth.token(registrationAccessCode));
        createdClients.put(clientRepresentation.getClientId(), new ClientData(clientRepresentation, registrationAccessCode));
        return this;
    }

    /**
     * Creates a direct grant client.
     *
     * @see {@link #createClient(ClientRepresentation)}
     */
    public FluentTestsHelper createDirectGrantClient(String clientId) throws ClientRegistrationException, JsonProcessingException {
        assert isInitialized;
        createClient(ClientBuilder.create(clientId).accessType(PUBLIC));
        return this;
    }

    /**
     * Deletes a client previously created by this helper. This will throw an error if you try to delete an
     * arbitrary client.
     *
     * @param clientId Client id to be deleted.
     * @return <code>this</code>
     * @throws ClientRegistrationException Thrown when client registration error occurs.
     */
    public FluentTestsHelper deleteClient(String clientId) throws ClientRegistrationException {
        assert isInitialized;
        ClientData clientData = createdClients.get(clientId);
        if (clientData == null) {
            throw new ClientRegistrationException("This client wasn't created by this helper!");
        }
        ClientRegistration reg = ClientRegistration.create()
                .url(keycloakBaseUrl, testRealm)
                .build();
        reg.auth(Auth.token(clientData.getRegistrationCode()));
        reg.delete(clientId);
        return this;
    }

    /**
     * @see #importTestRealm(InputStream)
     */
    public FluentTestsHelper importTestRealm(String realmJsonPath) throws IOException {
        try (InputStream fis = FluentTestsHelper.class.getResourceAsStream(realmJsonPath)) {
            return importTestRealm(fis);
        }
    }

    /**
     * @see #importTestRealm(InputStream)
     */
    public FluentTestsHelper importTestRealm(File realmJsonPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(realmJsonPath)) {
            return importTestRealm(fis);
        }
    }

    /**
     * Import a test realm.
     *
     * @param stream A stream representing a JSON file with an exported realm.
     * @return <code>this</code>
     * @throws IOException Thrown in case of parsing error.
     */
    public FluentTestsHelper importTestRealm(InputStream stream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        RealmRepresentation realmRepresentation = mapper.readValue(stream, RealmRepresentation.class);
        return createTestRealm(realmRepresentation);
    }

    /**
     * Creates a test realm.
     *
     * @param realmRepresentation A test realm representation.
     * @return <code>this</code>
     */
    public FluentTestsHelper createTestRealm(RealmRepresentation realmRepresentation) {
        assert isInitialized;
        keycloak.realms().create(realmRepresentation);
        testRealm = realmRepresentation.getRealm();
        accessToken = generateInitialAccessToken();
        return this;
    }

    /**
     * Deletes a realm.
     *
     * @param realmName Realm to be deleted.
     * @return <code>this</code>
     */
    public FluentTestsHelper deleteRealm(String realmName) {
        assert isInitialized;
        keycloak.realms().realm(realmName).remove();
        return this;
    }

    /**
     * Deletes the test realm. Meant to be called after testing has finished.
     *
     * @return <code>this</code>
     */
    public FluentTestsHelper deleteTestRealm() {
        deleteRealm(testRealm);
        return this;
    }

    /**
     * Creates a test user.
     *
     * @param username A username to be created.
     * @param password A password for a user.
     * @return <code>this</code>
     */
    public FluentTestsHelper createTestUser(String username, String password) {
        assert isInitialized;
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(username);
        userRepresentation.setEnabled(true);
        Response response = keycloak.realms().realm(testRealm).users().create(userRepresentation);
        String userId = getCreatedId(response);
        response.close();
        CredentialRepresentation rep = new CredentialRepresentation();
        rep.setType(CredentialRepresentation.PASSWORD);
        rep.setValue(password);
        rep.setTemporary(false);
        keycloak.realms().realm(testRealm).users().get(userId).resetPassword(rep);
        return this;
    }

    /**
     * Associates a user with a role. This method also creates a role if that is missing.
     *
     * @param userName A username to be associated with a role.
     * @param roleName A role to be associated with a user name.
     * @return <code>this</code>
     */
    public FluentTestsHelper assignRoleWithUser(String userName, String roleName) {
        assert isInitialized;
        if (keycloak.realms().realm(testRealm).roles().get(roleName) == null) {
            RoleRepresentation representation = new RoleRepresentation();
            representation.setName(roleName);
            keycloak.realms().realm(testRealm).roles().create(representation);
        }
        UserRepresentation userRepresentation = keycloak.realms().realm(testRealm).users().search(userName).get(0);
        RoleRepresentation realmRole =  keycloak.realms().realm(testRealm).roles().get(roleName).toRepresentation();
        keycloak.realms().realm(testRealm).users().get(userRepresentation.getId()).roles().realmLevel().add(Arrays.asList(realmRole));
        return this;
    }

    /**
     * Deletes a role.
     *
     * @param roleName A Role name to be deleted.
     * @return <code>this</code>
     */
    public FluentTestsHelper deleteRole(String roleName) {
        assert isInitialized;
        RoleResource role = keycloak.realms().realm(testRealm).roles().get(roleName);
        if (role != null) {
            keycloak.realms().realm(testRealm).roles().deleteRole(roleName);
        }
        return this;
    }

    /**
     * Deletes a user.
     *
     * @param userName A Username to be deleted.
     * @return <code>this</code>
     */
    public FluentTestsHelper deleteTestUser(String userName) {
        assert isInitialized;
        UserRepresentation userInKeycloak = keycloak.realms().realm(testRealm).users().search(userName).get(0);
        if (userInKeycloak != null) {
            keycloak.realms().realm(testRealm).users().delete(userInKeycloak.getId());
        }
        return this;
    }

    /**
     * @param response
     * @return ID of the created record
     */
    public String getCreatedId(Response response) {
        URI location = response.getLocation();
        if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
            Response.StatusType statusInfo = response.getStatusInfo();
            throw new WebApplicationException("Create method returned status "
                    + statusInfo.getReasonPhrase() + " (Code: " + statusInfo.getStatusCode() + "); expected status: Created (201)", response);
        }
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Checks if given endpoint returns successfully with supplied token.
     *
     * @param endpoint Endpoint to be evaluated,
     * @param token Token that will be passed into the <code>Authorization</code> header.
     * @return <code>true</code> if the endpoint returns forbidden.
     * @throws IOException Thrown by the underlying HTTP Client implementation
     */
    public boolean testGetWithAuth(String endpoint, String token) throws IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();

        try {
            HttpGet get = new HttpGet(keycloakBaseUrl + endpoint);
            get.addHeader("Authorization", "Bearer " + token);

            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() != 200) {
                return false;
            }
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            try {
                return true;
            } finally {
                is.close();
            }

        } finally {
            client.close();
        }
    }

    /**
     * Checks if a given endpoint returns Forbidden HTTP Code.
     *
     * @param endpoint Endpoint to be evaluated,
     * @return <code>true</code> if the endpoint returns forbidden.
     * @throws IOException Thrown by the underlying HTTP Client implementation
     */
    public boolean returnsForbidden(String endpoint) throws IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        try {
            HttpGet get = new HttpGet(keycloakBaseUrl + endpoint);
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == 403 || response.getStatusLine().getStatusCode() == 401) {
                return true;
            } else {
                return false;
            }

        } finally {
            client.close();
        }
    }

    /**
     * @return Returns an Access Token.
     */
    public String getToken() {
        assert isInitialized;
        return keycloak.tokenManager().getAccessTokenString();
    }

    public String getKeycloakBaseUrl() {
        return keycloakBaseUrl;
    }

    public String getAdminUserName() {
        return adminUserName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String getAdminClientId() {
        return adminClient;
    }

    public String getAdminRealmName() {
        return adminRealm;
    }

    public String getTestRealmName() {
        return testRealm;
    }

    public RealmResource getTestRealmResource() {
        assert isInitialized;
        return keycloak.realm(testRealm);
    }

    @Override
    public void close() {
        if (keycloak != null && !keycloak.isClosed()) {
            keycloak.close();
        }
        isInitialized = false;
    }
}
