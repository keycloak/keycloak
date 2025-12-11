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
package org.keycloak.quarkus.runtime.configuration;

import java.util.Map;

import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.configuration.mappers.ManagementPropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ManagementConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void managementDefaults() {
        initConfig();

        assertConfig(Map.of(
                "http-management-port", "9000",
                "http-management-relative-path", "/",
                "http-management-host", "0.0.0.0"
        ));

        assertManagementEnabled(false);
    }

    @Test
    public void healthOccupied() {
        assertOccupied("KC_HEALTH_ENABLED");
    }

    @Test
    public void metricsOccupied() {
        assertOccupied("KC_METRICS_ENABLED");
    }

    @Test
    public void healthMetricsOccupied() {
        assertOccupied("KC_HEALTH_ENABLED", "KC_METRICS_ENABLED");
    }

    @Test
    public void immutableManagementEnabledProperty() {
        initConfig();
        assertConfig("http-management-enabled", "false");

        putEnvVar("KC_MANAGEMENT_ENABLED", "true");

        initConfig();
        assertConfig("http-management-enabled", "false");

        putEnvVar("KC_MANAGEMENT_ENABLED", "something-wrong");

        initConfig();
        assertConfig("http-management-enabled", "false");
    }

    @Test
    public void managementBasicChanges() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTP_MANAGEMENT_PORT", "9999",
                "KC_HTTP_MANAGEMENT_RELATIVE_PATH", "/management2",
                "KC_HTTP_MANAGEMENT_HOST", "somehost"
        ));

        initConfig();

        assertConfig(Map.of(
                "http-management-port", "9999",
                "http-management-relative-path", "/management2",
                "http-relative-path", "/",
                "http-management-host", "somehost"
        ));
        assertManagementEnabled(true);
    }

    @Test
    public void managementRelativePath() {
        makeInterfaceOccupied();
        putEnvVar("KC_HTTP_RELATIVE_PATH", "/management3");

        initConfig();

        assertConfig(Map.of(
                "http-management-relative-path", "/management3",
                "http-relative-path", "/management3"
        ));
        assertManagementEnabled(true);
    }

    @Test
    public void managementHttpsValues() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTP_MANAGEMENT_HOST", "host1",
                "KC_HTTPS_MANAGEMENT_CLIENT_AUTH", "requested",
                "KC_HTTPS_MANAGEMENT_CIPHER_SUITES", "some-cipher-suite1",
                "KC_HTTPS_MANAGEMENT_PROTOCOLS", "TLSv1.3",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_FILE", "/some/path/s.crt.pem",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE", "/some/path/s.key.pem",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_FILE", "keystore123.p12",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_PASSWORD", "ultra-password123",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_TYPE", "BCFKS-0.1"
        ));

        initConfig();

        assertConfig(Map.of(
                "http-management-host", "host1",
                "https-management-client-auth", "requested",
                "https-management-cipher-suites", "some-cipher-suite1",
                "https-management-protocols", "TLSv1.3",
                "https-management-certificate-file", "/some/path/s.crt.pem",
                "https-management-certificate-key-file", "/some/path/s.key.pem",
                "https-management-key-store-file", "keystore123.p12",
                "https-management-key-store-password", "ultra-password123",
                "https-management-key-store-type", "BCFKS-0.1"
        ));
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementMappedValues() {
        makeInterfaceOccupied();
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
                "http-management-host", "host123",
                "https-management-client-auth", "required",
                "https-management-cipher-suites", "some-cipher-suite",
                "https-management-protocols", "TLSv1.2",
                "https-management-certificate-file", "/some/path/srv.crt.pem",
                "https-management-certificate-key-file", "/some/path/srv.key.pem",
                "https-management-key-store-file", "keystore.p12",
                "https-management-key-store-password", "ultra-password",
                "https-management-key-store-type", "BCFKS"
        ));
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementDefaultHttps() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/some/path/srv.crt.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/some/path/srv.key.pem"
        ));

        initConfig();

        assertConfig(Map.of(
                "https-certificate-file", "/some/path/srv.crt.pem",
                "https-certificate-key-file", "/some/path/srv.key.pem",
                "https-management-certificate-file", "/some/path/srv.crt.pem",
                "https-management-certificate-key-file", "/some/path/srv.key.pem"
        ));
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementSchemeHttp() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/some/path/srv.crt.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/some/path/srv.key.pem",
                "KC_HTTP_MANAGEMENT_SCHEME", "http"
        ));

        initConfig();
        PropertyMappers.sanitizeDisabledMappers(new Build());

        assertConfig(Map.of(
                "https-certificate-file", "/some/path/srv.crt.pem",
                "https-certificate-key-file", "/some/path/srv.key.pem"
        ));
        assertConfigNull("https-management-certificate-file");
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(false);
    }

    @Test
    public void managementDefaultHttpsManagementProps() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_FILE", "/some/path/srv.crt.pem",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE", "/some/path/srv.key.pem"
        ));

        initConfig();

        assertConfig(Map.of(
                "https-management-certificate-file", "/some/path/srv.crt.pem",
                "https-management-certificate-key-file", "/some/path/srv.key.pem"
        ));
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementDefaultHttpsCertDisabled() {
        makeInterfaceOccupied();
        putEnvVar("KC_HTTPS_CERTIFICATE_FILE", "/some/path/srv.crt.pem");

        initConfig();

        assertConfig("https-management-certificate-file", "/some/path/srv.crt.pem");
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(false);
    }

    @Test
    public void managementDefaultHttpsKeyDisabled() {
        makeInterfaceOccupied();
        putEnvVar("KC_HTTPS_CERTIFICATE_KEY_FILE", "/some/path/srv.key.pem");

        initConfig();

        assertConfig("https-management-certificate-key-file", "/some/path/srv.key.pem");
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(false);
    }

    @Test
    public void managementDefaultHttpsCertificatesReload() {
        makeInterfaceOccupied();
        putEnvVar("KC_HTTPS_CERTIFICATES_RELOAD_PERIOD", "2d");

        initConfig();

        assertConfig("https-management-certificates-reload-period", "2d");
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(false);
    }

    @Test
    public void managementEnabledDefaultHttpsKeystore(){
        makeInterfaceOccupied();
        putEnvVar("KC_HTTPS_KEY_STORE_FILE", "keystore.p12");

        initConfig();

        assertConfig(Map.of(
                "https-key-store-file", "keystore.p12",
                "https-management-key-store-file", "keystore.p12"
        ));
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void fipsKeystoreType(){
        makeInterfaceOccupied();
        putEnvVar("KC_FIPS_MODE", "strict");

        initConfig();

        assertConfig(Map.of(
                "https-key-store-type", "BCFKS",
                "https-management-key-store-type", "BCFKS"
        ));
        assertManagementEnabled(true);
    }

    @Test
    public void keystoreType(){
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_KEY_STORE_TYPE", "pkcs12",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_TYPE", "BCFKS"
        ));

        initConfig();

        assertConfig(Map.of(
                "https-key-store-type", "pkcs12",
                "https-management-key-store-type", "BCFKS"
        ));
        assertManagementEnabled(true);
    }

    @Test
    public void legacyObservabilityInterface() {
        makeInterfaceOccupied();
        putEnvVar("KC_LEGACY_OBSERVABILITY_INTERFACE", "true");

        initConfig();

        assertConfig("legacy-observability-interface", "true");
        assertManagementEnabled(false);
    }

    @Test
    public void legacyObservabilityInterfaceFalse() {
        makeInterfaceOccupied();
        putEnvVar("KC_LEGACY_OBSERVABILITY_INTERFACE", "false");

        initConfig();

        assertConfig("legacy-observability-interface", "false");
        assertManagementEnabled(true);
    }

    private void makeInterfaceOccupied() {
        putEnvVar("KC_HEALTH_ENABLED", "true");
    }

    private void assertManagementEnabled(boolean expected) {
        assertThat("Expected value for Management interface state is different", ManagementPropertyMappers.isManagementEnabled(), is(expected));
    }

    private void assertManagementHttpsEnabled(boolean expected) {
        assertThat("Expected value for Management HTTPS is different", ManagementPropertyMappers.isManagementTlsEnabled(), is(expected));
    }

    private void assertOccupied(String... envVarChangeState) {
        for (var env : envVarChangeState) {
            putEnvVar(env, "true");
        }

        initConfig();

        assertConfig(Map.of(
                "http-management-port", "9000",
                "http-management-relative-path", "/",
                "http-management-host", "0.0.0.0"
        ));

        assertManagementEnabled(true);
        assertManagementHttpsEnabled(false);
    }
}
