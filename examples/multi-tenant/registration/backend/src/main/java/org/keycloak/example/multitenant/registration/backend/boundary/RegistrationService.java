/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.example.multitenant.registration.backend.boundary;

import org.keycloak.example.multitenant.registration.backend.entity.RegistrationRequest;
import org.keycloak.example.multitenant.registration.backend.entity.RegistrationResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.UUID;
import javax.ejb.Stateless;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.io.JsonStringEncoder;
import static org.keycloak.ServiceUrlConstants.REALM_INFO_PATH;
import static org.keycloak.ServiceUrlConstants.TOKEN_SERVICE_DIRECT_GRANT_PATH;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
@Stateless
@Path("/registration")
public class RegistrationService {

    private static final String KEYCLOAK_BASE_URL = "http://localhost:8180/auth";
    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);
    private static final JsonStringEncoder jsonEncoder = JsonStringEncoder.getInstance();

    private static final String KEYCLOAK_USERNAME = "registration";
    private static final String KEYCLOAK_PASSWORD = "registration";
    private static final String KEYCLOAK_OAUTH_CLIENT = "registration";
    private static final String KEYCLOAK_OAUTH_SECRET = "fc4dd5bc-a2cb-4d40-98fe-98a999c36767";

    private static final String KEYCLOAK_ADMIN_APPLICATION_INSTALLATION_JSON = "/admin/realms/{realm}/applications/{app-name}/installation/json";

    @POST
    public RegistrationResponse register(RegistrationRequest bean) throws MalformedURLException, IOException, URISyntaxException {
        String oauthSecret = UUID.randomUUID().toString();
        String nodeUsername = UUID.randomUUID().toString();
        String nodePassword = new BigInteger(130, new SecureRandom()).toString(32);

        InputStream realmJsonIs = getClass().getResourceAsStream("/realm-template.json");
        Scanner scanner = new java.util.Scanner(realmJsonIs).useDelimiter("\\A");
        String realmJsonTemplate = scanner.hasNext() ? scanner.next() : "";

        String realmJsonFormatted = String.format(
                realmJsonTemplate,
                new String(jsonEncoder.quoteAsString(bean.getAccountName())),
                new String(jsonEncoder.quoteAsString(bean.getAdminUsername())),
                new String(jsonEncoder.quoteAsString(bean.getAdminPassword())),
                nodeUsername,
                nodePassword,
                oauthSecret);

        AccessTokenResponse token = getToken();

        RegistrationResponse response;
        if (null == token) {
            return new RegistrationResponse("Couldn't contact the Keycloak server. Try again later.");
        }

        if (!accountNameAvailable(token, bean.getAccountName())) {
            return new RegistrationResponse("There's already an account with this name. Please, choose a different name");
        }

        String authorization = String.format("Bearer %s", token.getToken());

        URL createRealmURL = new URL(KEYCLOAK_BASE_URL + "/admin/realms");
        HttpURLConnection connection = (HttpURLConnection) (createRealmURL.openConnection());
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setReadTimeout(5000); // 5 seconds
        connection.setConnectTimeout(5000); // 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        try (OutputStream output = connection.getOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8")) {
            writer.write(realmJsonFormatted);
        }

        connection.connect();

        int code = connection.getResponseCode();

        if (code == 201) {
            String applicationJson = getApplicationJson(bean.getAccountName(), "metrics");
            response = new RegistrationResponse(nodeUsername, nodePassword, oauthSecret, applicationJson);
        } else {
            response = new RegistrationResponse(String.format("Couldn't register a new realm. Reason: %s", connection.getResponseMessage()));
        }

        return response;
    }

    private String getApplicationJson(String accountName, String applicationName) throws IOException {
        String authorization = String.format("Bearer %s", getToken().getToken());
        URL retrieveApplicationJsonURL = KeycloakUriBuilder
                .fromUri(KEYCLOAK_BASE_URL)
                .path(KEYCLOAK_ADMIN_APPLICATION_INSTALLATION_JSON)
                .build(accountName, applicationName)
                .toURL();

        HttpURLConnection connection = (HttpURLConnection) (retrieveApplicationJsonURL.openConnection());
        connection.setReadTimeout(5000); // 5 seconds
        connection.setConnectTimeout(5000); // 5 seconds
        connection.setRequestProperty("Authorization", authorization);
        connection.connect();

        int code = connection.getResponseCode();

        if (code == 200) {
            try (InputStream keycloakResponse = connection.getInputStream()) {
                Scanner scanner = new Scanner(keycloakResponse).useDelimiter("\\A");
                String applicationJson = scanner.hasNext() ? scanner.next() : "";
                return applicationJson;
            }
        } else {
            logger.error("Error while trying to retrieve the application JSON from Keycloak. Reason: HTTP status code " + code);
            return null;
        }
    }

    private boolean accountNameAvailable(AccessTokenResponse token, String accountName) throws MalformedURLException, IOException {
        URL createRealmURL = KeycloakUriBuilder.fromUri(KEYCLOAK_BASE_URL).path(REALM_INFO_PATH).build(accountName).toURL();
        String authorization = String.format("Bearer %s", token.getToken());

        HttpURLConnection connection = (HttpURLConnection) (createRealmURL.openConnection());
        connection.setReadTimeout(5000); // 5 seconds
        connection.setConnectTimeout(5000); // 5 seconds
        connection.setRequestProperty("Authorization", authorization);
        connection.connect();

        int code = connection.getResponseCode();
        connection.disconnect();
        // being conservative here: the only status code we are interested is 404,
        // anything else we consider an error and we report as if the account exists
        return code == 404;
    }

    private AccessTokenResponse getToken() throws IOException {
        URI keycloakUri = KeycloakUriBuilder.fromUri(KEYCLOAK_BASE_URL).path(TOKEN_SERVICE_DIRECT_GRANT_PATH).build("master");
        String authorization = BasicAuthHelper.createHeader(KEYCLOAK_OAUTH_CLIENT, KEYCLOAK_OAUTH_SECRET);

        HttpURLConnection connection = (HttpURLConnection) (keycloakUri.toURL().openConnection());
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setReadTimeout(5000); // 5 seconds
        connection.setConnectTimeout(5000); // 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", authorization);

        try (OutputStream output = connection.getOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8")) {
            writer.write(String.format("username=%s&password=%s", KEYCLOAK_USERNAME, KEYCLOAK_PASSWORD));
        }

        connection.connect();

        int code = connection.getResponseCode();

        if (code == 200) {
            try (InputStream keycloakResponse = connection.getInputStream()) {
                return JsonSerialization.readValue(keycloakResponse, AccessTokenResponse.class);
            }
        } else {
            logger.error("Error while trying to retrieve the access token: {}", connection.getResponseMessage());
        }

        return null;
    }

}
