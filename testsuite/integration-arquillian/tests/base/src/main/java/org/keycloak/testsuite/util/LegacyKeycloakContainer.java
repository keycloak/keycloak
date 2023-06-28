/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.keycloak.util.JsonSerialization;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class LegacyKeycloakContainer extends GenericContainer<LegacyKeycloakContainer> {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    public LegacyKeycloakContainer(String tagName) {
        super("quay.io/keycloak/keycloak" + getManifestDigestOfImageByName(tagName));

        withEnv("KEYCLOAK_ADMIN", USERNAME);
        withEnv("KEYCLOAK_ADMIN_PASSWORD", PASSWORD);
        withNetworkMode("host");

        //order of waitingFor and withStartupTimeout matters as the latter sets the timeout for WaitStrategy set by waitingFor
        waitingFor(Wait.forLogMessage(".*Running the server in development mode..*", 1));
        withStartupTimeout(Duration.ofMinutes(5));
    }

    /**
     * For tag names as "latest" or "nightly" it may happen that some older version keycloak could be cached.
     * Therefore fetching container by its name wouldn't be reliable.
     * 
     * This method obtains image "manifest_digest" by the name from quay.io and returns it with prefix "@".
     * It can be then used to fetch the container with: 
     * 
     * <p>quay.io/keycloak/keycloak@sha256:...</p>
     * 
     * @param tagName Name of the image tag.
     * @return "manifest_digest" of the image if "latest" or "nightly" with prefix "@", {@code tagName} with ":" prefix otherwise
     */
    private static String getManifestDigestOfImageByName(String tagName) {
        if ("latest".equals(tagName) || "nightly".equals(tagName)) {

            try {
                URI uri = new URI("https://quay.io/api/v1/repository/keycloak/keycloak/tag/?specificTag=" + tagName);
                HttpResponse<String> response = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build()
                        .send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    JsonNode manifestDigest = JsonSerialization.mapper.readTree(response.body()).findValue("manifest_digest");
                    if (manifestDigest != null) {
                        return "@" + manifestDigest.asText();
                    }
                }
                throw new RuntimeException(String.format("Unable to get manifest_digest for image with tag %s from quay.io. Response: %d, %s. ", tagName, response.statusCode(), response.body()));
            } catch (URISyntaxException | IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }

        } else {
            return ":" + tagName;
        }
    }
}
