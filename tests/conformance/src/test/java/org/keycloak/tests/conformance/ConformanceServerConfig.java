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

package org.keycloak.tests.conformance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.common.Profile;
import org.keycloak.testframework.conformance.ConformanceTls;
import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

/**
 * Keycloak server configuration for the OpenID provider conformance tests (OpenID Connect). They share it so the
 * server is not restarted between them; the settings one plan needs do not affect the others:
 * <ul>
 * <li>the BCP195 TLS restriction FAPI requires</li>
 * <li>trust in the suite's TLS certificate for the requests Keycloak sends to the suite, e.g. for the jwks_uri of a
 * dynamically registered client.
 * Keycloak reaches the suite on the Docker host, whose name the self-signed certificate does not cover, hence no
 * hostname verification. This is acceptable for a test server only.</li>
 * <li>the requests Keycloak sends to the URLs the suite publishes on its container internal host, e.g. the jwks_uri,
 * request_uri or sector_identifier_uri of a dynamically registered client, go through a tunnel to the suite, see
 * {@link OpenIdConformanceServer#keycloakProxyMapping()}</li>
 * <li>the conformance test adjustment of the ID tokens, a script based protocol mapper of this module
 * (src/main/resources/conformance-adjustment-oidc-id-token.js), see there for what it changes and why</li>
 * </ul>
 */
public class ConformanceServerConfig implements KeycloakServerConfig {

    // Written once, as a different path on every configuration would restart the server for every test class
    private static Path truststore;

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        // The scripts feature and the deployment of this module as a provider are only there for the conformance test
        // adjustment of the ID tokens, which is a script based protocol mapper of this module
        return config.features(Profile.Feature.SCRIPTS)
                .dependencyCurrentProject()
                .option("https-protocols", ConformanceTls.PROTOCOLS)
                .option("https-cipher-suites", ConformanceTls.BCP195_CIPHERS)
                .option("hostname", OpenIdConformanceServer.KEYCLOAK_BASE_URI.toString())
                .option("truststore-paths", truststore().toString())
                .option("tls-hostname-verifier", "ANY")
                .option("spi-connections-http-client--default--proxy-mappings",
                        OpenIdConformanceServer.instance().keycloakProxyMapping());
    }

    private static synchronized Path truststore() {
        if (truststore == null) {
            try {
                Path path = Files.createTempFile("keycloak-conformance-truststore", ".pem");
                path.toFile().deleteOnExit();
                Files.writeString(path, OpenIdConformanceServer.instance().certificatePem() + "\n");
                truststore = path;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write the conformance truststore", e);
            }
        }
        return truststore;
    }
}
