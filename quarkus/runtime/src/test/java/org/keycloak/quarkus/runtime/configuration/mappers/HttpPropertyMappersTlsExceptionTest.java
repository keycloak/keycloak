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
package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.Map;

import org.keycloak.config.HttpOptions;
import org.keycloak.config.ManagementOptions;
import org.keycloak.quarkus.runtime.configuration.AbstractConfigurationTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpPropertyMappersTlsExceptionTest extends AbstractConfigurationTest {

    @Test
    public void resolveHttpTrustStoreTypeOption() {
        createConfigFromCliArguments("--https-trust-store-file=not-there.ks", "--health-enabled=true");

        assertThat(resolveTrustStoreTypeOption("not-there.ks"), is(HttpOptions.HTTPS_TRUST_STORE_TYPE.getKey()));
    }

    @Test
    public void resolveManagementTrustStoreTypeOption() {
        putEnvVar("KC_HEALTH_ENABLED", "true");
        createConfigFromCliArguments(
                "--https-trust-store-file=shared.ks",
                "--https-management-trust-store-file=management-trust.ks");

        assertThat(resolveTrustStoreTypeOption("management-trust.ks"),
                is(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_TYPE.getKey()));
    }

    @Test
    public void resolveInheritedTrustStoreTypeOptionUsesHttp() {
        putEnvVars(Map.of(
                "KC_HEALTH_ENABLED", "true",
                "KC_HTTPS_TRUST_STORE_FILE", "shared.ks"
        ));
        createConfig();

        assertThat(resolveTrustStoreTypeOption("shared.ks"), is(HttpOptions.HTTPS_TRUST_STORE_TYPE.getKey()));
    }

    @Test
    public void resolveManagementKeyStoreTypeOption() {
        putEnvVar("KC_HEALTH_ENABLED", "true");
        createConfigFromCliArguments(
                "--https-key-store-file=shared.ks",
                "--https-management-key-store-file=management.ks");

        assertThat(resolveKeyStoreTypeOption("management.ks"), is(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE.getKey()));
    }

    private static String resolveTrustStoreTypeOption(String path) {
        String message = trustStoreTypeMessage(path);
        return HttpPropertyMappers.resolveStoreTypeOption(message,
                "quarkus.http.ssl.certificate.trust-store-file",
                ManagementPropertyMappers.QUARKUS_MANAGEMENT_HTTPS_TRUST_STORE_FILE,
                HttpOptions.HTTPS_TRUST_STORE_TYPE, ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_TYPE,
                ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_FILE);
    }

    private static String resolveKeyStoreTypeOption(String path) {
        String message = keyStoreTypeMessage(path);
        return HttpPropertyMappers.resolveStoreTypeOption(message,
                "quarkus.http.ssl.certificate.key-store-file",
                ManagementPropertyMappers.QUARKUS_MANAGEMENT_HTTPS_KEY_STORE_FILE,
                HttpOptions.HTTPS_KEY_STORE_TYPE, ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE,
                ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE);
    }

    private static String trustStoreTypeMessage(String path) {
        return "Could not determine the truststore type from the file name: " + path
                + ". Configure the `quarkus.http.ssl.certificate.trust-store-file-type` property.";
    }

    private static String keyStoreTypeMessage(String path) {
        return "Could not determine the keystore type from the file name: " + path
                + ". Configure the `quarkus.http.ssl.certificate.key-store-file-type` property.";
    }
}
