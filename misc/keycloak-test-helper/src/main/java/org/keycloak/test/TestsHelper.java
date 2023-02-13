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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.admin.client.Keycloak;
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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

import static org.keycloak.test.builders.ClientBuilder.AccessType.PUBLIC;

/**
 * @deprecated This class will be removed in the next major release. Please migrate to {@link FluentTestsHelper}.
 */
@Deprecated
public class TestsHelper {

    public static String baseUrl;

    public static String keycloakBaseUrl = "http://localhost:8180/auth";

    public static String testRealm = "test-realm";

    public static String initialAccessCode;

    public static String appName;
    
    public static int initialAccessTokenCount = 2; 

    protected static String clientConfiguration;

    protected static String registrationAccessCode;

    public static String createClient(ClientRepresentation clientRepresentation) {
        ClientRegistration reg = ClientRegistration.create()
                .url(keycloakBaseUrl, testRealm)
                .build();

        reg.auth(Auth.token(initialAccessCode));
        try {
            clientRepresentation = reg.create(clientRepresentation);
            registrationAccessCode = clientRepresentation.getRegistrationAccessToken();
            ObjectMapper mapper = new ObjectMapper();
            reg.auth(Auth.token(registrationAccessCode));
            clientConfiguration = mapper.writeValueAsString(reg.getAdapterConfig(clientRepresentation.getClientId()));
        } catch (ClientRegistrationException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return clientConfiguration;
    }
    
    public static String createDirectGrantClient() {
        return createClient(ClientBuilder.create("test-dga").accessType(PUBLIC));
    }

    public static void deleteClient(String clientId) {
        ClientRegistration reg = ClientRegistration.create()
                .url(keycloakBaseUrl, testRealm)
                .build();
        try {
            reg.auth(Auth.token(registrationAccessCode));
            reg.delete(clientId);
        } catch (ClientRegistrationException e) {
            e.printStackTrace();
        }
    }

    public static boolean testGetWithAuth(String endpoint, String token) throws IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
       
        try {
            HttpGet get = new HttpGet(baseUrl + endpoint);
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

    public static boolean returnsForbidden(String endpoint) throws IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        try {
            HttpGet get = new HttpGet(baseUrl + endpoint);
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

    public static String getToken(String username, String password, String realm) {
        Keycloak keycloak = Keycloak.getInstance(
                keycloakBaseUrl,
                realm,
                username,
                password,
                "test-dga");
        return keycloak.tokenManager().getAccessTokenString();

    }

    public static boolean importTestRealm(String username, String password, String realmJsonPath) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        try (InputStream stream = TestsHelper.class.getResourceAsStream(realmJsonPath)) {
            RealmRepresentation realmRepresentation = mapper.readValue(stream, RealmRepresentation.class);

            Keycloak keycloak = Keycloak.getInstance(
                    keycloakBaseUrl,
                    "master",
                    username,
                    password,
                    "admin-cli");
            keycloak.realms().create(realmRepresentation);
            testRealm = realmRepresentation.getRealm();
            generateInitialAccessToken(keycloak);
            return true;
        }

    }

    public static boolean importTestRealm(String username, String password) throws IOException {
        testRealm = appName + "-realm";
        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setRealm(testRealm);
        realmRepresentation.setEnabled(true);
        Keycloak keycloak = Keycloak.getInstance(
                keycloakBaseUrl,
                "master",
                username,
                password,
                "admin-cli");
        keycloak.realms().create(realmRepresentation);
        generateInitialAccessToken(keycloak);
        return true;
    }

    private static void generateInitialAccessToken(Keycloak keycloak) {
        ClientInitialAccessCreatePresentation rep = new ClientInitialAccessCreatePresentation();
        rep.setCount(initialAccessTokenCount);
        rep.setExpiration(100);
        ClientInitialAccessPresentation initialAccess = keycloak.realms().realm(testRealm).clientInitialAccess().create(rep);
        initialAccessCode = initialAccess.getToken();
    }

    public static boolean deleteRealm(String username, String password, String realmName) throws IOException {

        Keycloak keycloak = Keycloak.getInstance(
                keycloakBaseUrl,
                "master",
                username,
                password,
                "admin-cli");
        keycloak.realms().realm(realmName).remove();
        return true;

    }

    public static boolean createTestUser(String username, String password, String realmName) throws IOException {

        Keycloak keycloak = Keycloak.getInstance(
                keycloakBaseUrl,
                "master",
                username,
                password,
                "admin-cli");
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(username);
        userRepresentation.setEnabled(Boolean.TRUE);
        Response response = keycloak.realms().realm(realmName).users().create(userRepresentation);
        String userId = getCreatedId(response);
        response.close();
        CredentialRepresentation rep = new CredentialRepresentation();
        rep.setType(CredentialRepresentation.PASSWORD);
        rep.setValue(password);
        rep.setTemporary(false);
        keycloak.realms().realm(realmName).users().get(userId).resetPassword(rep);
        //add roles
        RoleRepresentation representation = new RoleRepresentation();
        representation.setName("user");

        keycloak.realms().realm(realmName).roles().create(representation);
        RoleRepresentation realmRole =  keycloak.realms().realm(realmName).roles().get("user").toRepresentation();
        keycloak.realms().realm(realmName).users().get(userId).roles().realmLevel().add(Arrays.asList(realmRole));
        return true;

    }

    public static String getCreatedId(Response response) {
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
    
}
