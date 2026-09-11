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

package org.keycloak.tests.conformance.vci.haip.configs;

import org.keycloak.common.Profile;
import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.conformance.ConformanceSigningKey;
import org.keycloak.tests.conformance.vci.VciConformanceRealmUtil;

public class HaipVciServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.features(Profile.Feature.OID4VC_VCI, Profile.Feature.CLIENT_AUTH_ABCA)
                .option("https-protocols", VciConformanceRealmUtil.TLS_PROTOCOLS)
                .option("https-cipher-suites", VciConformanceRealmUtil.BCP195_CIPHERS)
                .option("hostname", OpenIdConformanceServer.KEYCLOAK_BASE_URI.toString())
                .spiOption("keys", "java-keystore", "keystores-path", ConformanceSigningKey.keystoresBaseDir());
    }
}
