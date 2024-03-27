/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.runtime.configuration.test;

import org.junit.Test;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.mappers.ManagementPropertyMappers;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ManagementConfigurationTest extends ConfigurationTest {

    @Test
    public void managementDefaults() {
        initConfig();

        assertConfig(Map.of(
                "management-enabled", "true",
                "management-port", "9000",
                "management-relative-path", "/",
                "management-host", "0.0.0.0"
        ));

        assertManagementHttpsEnabled(false);
    }

    @Test
    public void managementBasicChanges() {
        putEnvVars(Map.of(
                "KC_MANAGEMENT_PORT", "9999",
                "KC_MANAGEMENT_RELATIVE_PATH", "/management2",
                "KC_MANAGEMENT_HOST", "somehost"
        ));

        initConfig();

        assertConfig(Map.of(
                "management-enabled", "true",
                "management-port", "9999",
                "management-relative-path", "/management2",
                "management-host", "somehost"
        ));
    }

    @Test
    public void managementHttpsValues() {
        putEnvVars(Map.of(
                "KC_MANAGEMENT_ENABLED", "true",
                "KC_MANAGEMENT_HOST", "host1",
                "KC_MANAGEMENT_HTTPS_CLIENT_AUTH", "requested",
                "KC_MANAGEMENT_HTTPS_CIPHER_SUITES", "some-cipher-suite1",
                "KC_MANAGEMENT_HTTPS_PROTOCOLS", "TLSv1.3",
                "KC_MANAGEMENT_HTTPS_CERTIFICATE_FILE", "/some/path/s.crt.pem",
                "KC_MANAGEMENT_HTTPS_CERTIFICATE_KEY_FILE", "/some/path/s.key.pem",
                "KC_MANAGEMENT_HTTPS_KEY_STORE_FILE", "keystore123.p12",
                "KC_MANAGEMENT_HTTPS_KEY_STORE_PASSWORD", "ultra-password123",
                "KC_MANAGEMENT_HTTPS_KEY_STORE_TYPE", "BCFKS-0.1"
        ));

        initConfig();

        assertConfig(Map.of(
                "management-enabled", "true",
                "management-host", "host1",
                "management-https-client-auth", "requested",
                "management-https-cipher-suites", "some-cipher-suite1",
                "management-https-protocols", "TLSv1.3",
                "management-https-certificate-file", "/some/path/s.crt.pem",
                "management-https-certificate-key-file", "/some/path/s.key.pem",
                "management-https-key-store-file", "keystore123.p12",
                "management-https-key-store-password", "ultra-password123",
                "management-https-key-store-type", "BCFKS-0.1"
        ));
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementMappedValues() {
        putEnvVars(Map.of(
                "KC_HTTP_HOST", "host123",
                "KC_HTTPS_CLIENT_AUTH", "required",
                "KC_HTTPS_CIPHER_SUITES", "some-cipher-suite",
                "KC_HTTPS_PROTOCOLS", "TLSv1.2",
                "KC_HTTPS_CERTIFICATE_FILE", "/some/path/srv.crt.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/some/path/srv.key.pem",
                "KC_HTTPS_KEY_STORE_FILE", "keystore.p12",
                "KC_HTTPS_KEY_STORE_PASSWORD", "ultra-password",
                "KC_HTTPS_KEY_STORE_TYPE", "BCFKS"
        ));

        initConfig();

        assertConfig(Map.of(
                "management-enabled", "true",
                "management-host", "host123",
                "management-https-client-auth", "required",
                "management-https-cipher-suites", "some-cipher-suite",
                "management-https-protocols", "TLSv1.2",
                "management-https-certificate-file", "/some/path/srv.crt.pem",
                "management-https-certificate-key-file", "/some/path/srv.key.pem",
                "management-https-key-store-file", "keystore.p12",
                "management-https-key-store-password", "ultra-password",
                "management-https-key-store-type", "BCFKS"
        ));
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementDefaultHttps() {
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/some/path/srv.crt.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/some/path/srv.key.pem"
        ));

        initConfig();

        assertConfig(Map.of(
                "management-enabled", "true",
                "https-certificate-file", "/some/path/srv.crt.pem",
                "https-certificate-key-file", "/some/path/srv.key.pem",
                "management-https-certificate-file", "/some/path/srv.crt.pem",
                "management-https-certificate-key-file", "/some/path/srv.key.pem"
        ));
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementDefaultHttpsManagementProps() {
        putEnvVars(Map.of(
                "KC_MANAGEMENT_HTTPS_CERTIFICATE_FILE", "/some/path/srv.crt.pem",
                "KC_MANAGEMENT_HTTPS_CERTIFICATE_KEY_FILE", "/some/path/srv.key.pem"
        ));

        initConfig();

        assertConfig(Map.of(
                "management-enabled", "true",
                "management-https-certificate-file", "/some/path/srv.crt.pem",
                "management-https-certificate-key-file", "/some/path/srv.key.pem"
        ));
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementDefaultHttpsCertDisabled() {
        putEnvVar("KC_HTTPS_CERTIFICATE_FILE", "/some/path/srv.crt.pem");

        initConfig();

        assertConfig(Map.of(
                "management-enabled", "true",
                "management-https-certificate-file", "/some/path/srv.crt.pem"
        ));
        assertManagementHttpsEnabled(false);
    }

    @Test
    public void managementDefaultHttpsKeyDisabled() {
        putEnvVar("KC_HTTPS_CERTIFICATE_KEY_FILE", "/some/path/srv.key.pem");

        initConfig();

        assertConfig(Map.of(
                "management-enabled", "true",
                "management-https-certificate-key-file", "/some/path/srv.key.pem"
        ));
        assertManagementHttpsEnabled(false);
    }

    @Test
    public void managementEnabledDefaultHttpsKeystore(){
        putEnvVar("KC_HTTPS_KEY_STORE_FILE", "keystore.p12");

        initConfig();

        assertConfig(Map.of(
                "management-enabled", "true",
                "https-key-store-file", "keystore.p12",
                "management-https-key-store-file", "keystore.p12"
        ));
        assertManagementHttpsEnabled(true);
    }

    private void assertManagementHttpsEnabled(boolean expected) {
        assertThat("Expected value for Management HTTPS is different", ManagementPropertyMappers.isManagementTlsEnabled(), is(expected));
    }

    private void assertConfig(Map<String, String> expectedValues) {
        expectedValues.forEach((key, expectedValue) -> {
            var value = Configuration.getKcConfigValue(key).getValue();
            assertThat(String.format("Value is null for key '%s'", key), value, notNullValue());
            assertThat(String.format("Different value for key '%s'", key), value, is(expectedValue));
        });
    }
}