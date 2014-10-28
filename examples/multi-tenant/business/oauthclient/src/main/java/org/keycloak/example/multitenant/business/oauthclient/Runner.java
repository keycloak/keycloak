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
package org.keycloak.example.multitenant.business.oauthclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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
public class Runner {
    // change from here:
    private final String REALM_NAME = "acme";
    private final String KEYCLOAK_OAUTH_CLIENT_SECRET = "475767ad-33a5-4250-be3c-dee598de9d51";
    private final String KEYCLOAK_NODE_USERNAME = "3e2ddc3b-00e7-4cdd-b566-d94f9383d170";
    private final String KEYCLOAK_NODE_PASSWORD = "i33stoddc6kijh2g8i5a200an3";
    // to here

    private final String KEYCLOAK_OAUTH_CLIENT_ID = "metrics-collector";

    private static final String KEYCLOAK_BASE_URL = "http://localhost:8180/auth";
    private static final String METRICS_BASE_URL = "http://localhost:8080/multitenant-business-backend/metrics";
    Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String args[]) throws IOException {
        Runner runner = new Runner();
        runner.run();
    }

    public void run() throws IOException {
        AccessTokenResponse token = getToken();
        String authorization = String.format("Bearer %s", token.getToken());
        URL recordMetricsUrl = new URL(METRICS_BASE_URL);
        HttpURLConnection connection = (HttpURLConnection) (recordMetricsUrl.openConnection());
        connection.setReadTimeout(5000); // 5 seconds
        connection.setConnectTimeout(5000); // 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-Keycloak-Realm", REALM_NAME);
        connection.setRequestProperty("Authorization", authorization);
        connection.connect();

        int code = connection.getResponseCode();

        if (code == 200) {
            logger.info("Success");
        } else {
            logger.info("Something wrong happened: " + connection.getResponseMessage());
        }

        connection.disconnect();
    }

    private AccessTokenResponse getToken() throws IOException {
        URI keycloakUri = KeycloakUriBuilder.fromUri(KEYCLOAK_BASE_URL).path(TOKEN_SERVICE_DIRECT_GRANT_PATH).build(REALM_NAME);
        String authorization = BasicAuthHelper.createHeader(KEYCLOAK_OAUTH_CLIENT_ID, KEYCLOAK_OAUTH_CLIENT_SECRET);

        HttpURLConnection connection = (HttpURLConnection) (keycloakUri.toURL().openConnection());
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setReadTimeout(5000); // 5 seconds
        connection.setConnectTimeout(5000); // 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", authorization);

        try (OutputStream output = connection.getOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8")) {
            writer.write(String.format("username=%s&password=%s", KEYCLOAK_NODE_USERNAME, KEYCLOAK_NODE_PASSWORD));
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
