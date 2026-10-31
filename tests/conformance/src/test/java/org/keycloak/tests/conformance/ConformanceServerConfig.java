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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * <li>the script based protocol mappers of this module (src/main/resources), which keep the ID tokens of the
 * conformance realms free of claims the suite warns about, e.g. Keycloak's typ claim</li>
 * </ul>
 * Subclasses can trust additional certificates, e.g. the CAs of TLS client certificates.
 */
public class ConformanceServerConfig implements KeycloakServerConfig {

    // Written once per configuration, as a different path on every configuration would restart the server for every
    // test class
    private static final Map<Class<?>, Path> TRUSTSTORES = new ConcurrentHashMap<>();

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
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

    /**
     * PEM encoded certificates the server trusts in addition to the suite's TLS certificate.
     */
    protected List<String> additionalTrustedCertificates() {
        return List.of();
    }

    private Path truststore() {
        return TRUSTSTORES.computeIfAbsent(getClass(), configClass -> {
            List<String> certificates = new ArrayList<>(additionalTrustedCertificates());
            certificates.add(OpenIdConformanceServer.instance().certificatePem());
            try {
                Path path = Files.createTempFile("keycloak-conformance-truststore", ".pem");
                path.toFile().deleteOnExit();
                Files.writeString(path, String.join("\n", certificates) + "\n");
                return path;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write the conformance truststore", e);
            }
        });
    }
}
