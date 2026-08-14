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
import org.keycloak.quarkus.runtime.configuration.mappers.HttpPropertyMappers;
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
                "http-management-relative-path", "/"
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
        putEnvVars(Map.of(
                "KC_HTTPS_TRUST_STORE_FILE", "truststore.p12",
                "KC_HTTPS_TRUST_STORE_PASSWORD", "trust-password",
                "KC_HTTPS_TRUST_STORE_TYPE", "PKCS12"
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
        assertConfig(Map.of(
                "https-management-trust-store-file", "truststore.p12",
                "https-management-trust-store-password", "trust-password",
                "https-management-trust-store-type", "PKCS12"
        ));
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.p12.path", "truststore.p12",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.p12.password", "trust-password"
        ));
        assertManagementEnabled(true);
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementMappedTrustStoreValues() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_MANAGEMENT_TRUST_STORE_FILE", "management-truststore.p12",
                "KC_HTTPS_MANAGEMENT_TRUST_STORE_PASSWORD", "management-trust-password",
                "KC_HTTPS_MANAGEMENT_TRUST_STORE_TYPE", "JKS"
        ));

        initConfig();

        assertConfig(Map.of(
                "https-management-trust-store-file", "management-truststore.p12",
                "https-management-trust-store-password", "management-trust-password",
                "https-management-trust-store-type", "JKS"
        ));
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.jks.path", "management-truststore.p12",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.jks.password", "management-trust-password"
        ));
        assertManagementEnabled(true);
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

    @Test
    public void managementTlsConfigNameInheritedFromHttp() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/key.pem"
        ));
        initConfig();
        assertExternalConfig("quarkus.management.tls-configuration-name", "keycloak-management-server");
    }

    @Test
    public void managementTlsConfigNameWithOwnCerts() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_FILE", "/mgmt-cert.pem",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE", "/mgmt-key.pem"
        ));
        initConfig();
        assertExternalConfig("quarkus.management.tls-configuration-name", "keycloak-management-server");
        assertManagementHttpsEnabled(true);
    }

    @Test
    public void managementTlsConfigNameDisabledForHttpScheme() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/key.pem",
                "KC_HTTP_MANAGEMENT_SCHEME", "http"
        ));
        initConfig();
        PropertyMappers.sanitizeDisabledMappers(new Build());
        assertExternalConfigNull("quarkus.management.tls-configuration-name");
    }

    @Test
    public void managementPemCertKeyInheritedFromHttp() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/http-cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/http-key.pem"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert", "/http-cert.pem",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.key", "/http-key.pem"
        ));
    }

    @Test
    public void managementPemCertKeyOverride() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/http-cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/http-key.pem",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_FILE", "/mgmt-cert.pem",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE", "/mgmt-key.pem"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert", "/mgmt-cert.pem",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.key", "/mgmt-key.pem"
        ));
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.cert", "/http-cert.pem",
                HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.key", "/http-key.pem"
        ));
    }

    @Test
    public void managementPemKeyFilePasswordInheritedFromHttp() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/key.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE_PASSWORD", "http-secret"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.password", "http-secret",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.password", "http-secret"
        ));
    }

    @Test
    public void managementPemKeyFilePasswordOverride() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/key.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE_PASSWORD", "http-secret",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE_PASSWORD", "mgmt-secret"
        ));
        initConfig();
        assertExternalConfig(HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.password", "http-secret");
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.password", "mgmt-secret");
    }

    @Test
    public void managementKeystoreDispatchInheritedPkcs12() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_KEY_STORE_FILE", "server.p12",
                "KC_HTTPS_KEY_STORE_PASSWORD", "pass"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path", "server.p12",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.password", "pass"
        ));
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.jks.path");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.other.path");
    }

    @Test
    public void managementKeystoreDispatchOwnJks() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_KEY_STORE_FILE", "server.p12",
                "KC_HTTPS_KEY_STORE_PASSWORD", "http-pass",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_FILE", "mgmt.jks",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_PASSWORD", "mgmt-pass"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "key-store.p12.path", "server.p12",
                HttpPropertyMappers.TLS_PREFIX + "key-store.p12.password", "http-pass",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.jks.path", "mgmt.jks",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.jks.password", "mgmt-pass"
        ));
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path");
    }

    @Test
    public void managementTrustStoreDispatchInherited() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_TRUST_STORE_FILE", "trust.p12",
                "KC_HTTPS_TRUST_STORE_PASSWORD", "trust-pass"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.p12.path", "trust.p12",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.p12.password", "trust-pass"
        ));
    }

    @Test
    public void managementCipherSuitesAndProtocolsInherited() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CIPHER_SUITES", "TLS_AES_256_GCM_SHA384",
                "KC_HTTPS_PROTOCOLS", "TLSv1.3"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "cipher-suites", "TLS_AES_256_GCM_SHA384",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "protocols", "TLSv1.3",
                HttpPropertyMappers.TLS_PREFIX + "cipher-suites", "TLS_AES_256_GCM_SHA384",
                HttpPropertyMappers.TLS_PREFIX + "protocols", "TLSv1.3"
        ));
    }

    @Test
    public void managementKeystoreTypeFilterOnlyOther() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_KEY_STORE_TYPE", "PKCS12"
        ));
        initConfig();
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.other.type");

        putEnvVars(Map.of(
                "KC_HTTPS_MANAGEMENT_KEY_STORE_TYPE", "BCFKS",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_FILE", "mgmt.bcfks",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_PASSWORD", "pass"
        ));
        initConfig();
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.other.type", "BCFKS");
    }

    @Test
    public void managementFipsKeystoreTypeMappedToOther() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_FIPS_MODE", "strict",
                "KC_HTTPS_KEY_STORE_FILE", "server.bcfks",
                "KC_HTTPS_KEY_STORE_PASSWORD", "pass",
                "KC_HTTPS_TRUST_STORE_FILE", "trust.bcfks"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "key-store.other.type", "BCFKS",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.other.type", "BCFKS",
                HttpPropertyMappers.TLS_PREFIX + "trust-store.other.type", "BCFKS",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.other.type", "BCFKS"
        ));
    }

    @Test
    public void managementDefaultProtocolsPreserved() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/key.pem"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "protocols", "TLSv1.3,TLSv1.2",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "protocols", "TLSv1.3,TLSv1.2"
        ));
    }

    @Test
    public void managementPemPasswordNotSetWhenNotSpecified() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/key.pem"
        ));
        initConfig();
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.password");
    }

    @Test
    public void managementTrustStoreOverrideNoCrossContamination() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_KEY_STORE_FILE", "server.p12",
                "KC_HTTPS_KEY_STORE_PASSWORD", "http-pass",
                "KC_HTTPS_TRUST_STORE_FILE", "http-trust.p12",
                "KC_HTTPS_TRUST_STORE_PASSWORD", "http-trust-pass",
                "KC_HTTPS_MANAGEMENT_TRUST_STORE_FILE", "mgmt-trust.jks",
                "KC_HTTPS_MANAGEMENT_TRUST_STORE_PASSWORD", "mgmt-trust-pass"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "trust-store.p12.path", "http-trust.p12",
                HttpPropertyMappers.TLS_PREFIX + "trust-store.p12.password", "http-trust-pass",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.jks.path", "mgmt-trust.jks",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.jks.password", "mgmt-trust-pass"
        ));
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.p12.path");
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path", "server.p12",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.password", "http-pass"
        ));
    }

    @Test
    public void managementReloadPeriodOverride() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/key.pem",
                "KC_HTTPS_CERTIFICATES_RELOAD_PERIOD", "2h",
                "KC_HTTPS_MANAGEMENT_CERTIFICATES_RELOAD_PERIOD", "30m"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.cert", "/cert.pem",
                HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.key", "/key.pem",
                HttpPropertyMappers.TLS_PREFIX + "reload-period", "2h",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert", "/cert.pem",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.key", "/key.pem",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "reload-period", "30m"
        ));
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

        putEnvVar("KC_HTTP_HOST", "0.0.0.0");

        initConfig();

        assertConfig(Map.of(
                "http-management-port", "9000",
                "http-management-relative-path", "/",
                "http-management-host", "0.0.0.0"
        ));

        assertManagementEnabled(true);
        assertManagementHttpsEnabled(false);
    }

    @Test
    public void managementInheritsPemFromHttp() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/http-cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/http-key.pem"
        ));
        initConfig();
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert", "/http-cert.pem");
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.key", "/http-key.pem");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path");
    }

    @Test
    public void managementInheritsKeystoreFromHttp() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_KEY_STORE_FILE", "server.p12",
                "KC_HTTPS_KEY_STORE_PASSWORD", "pass"
        ));
        initConfig();
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path", "server.p12");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert");
    }

    @Test
    public void explicitManagementKeystoreOverridesInheritedHttpPem() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/http-cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/http-key.pem",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_FILE", "mgmt.p12",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_PASSWORD", "mgmt-pass"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path", "mgmt.p12",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.password", "mgmt-pass"
        ));
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert");
    }

    @Test
    public void explicitManagementPemOverridesInheritedHttpKeystore() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_KEY_STORE_FILE", "server.p12",
                "KC_HTTPS_KEY_STORE_PASSWORD", "pass",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_FILE", "/mgmt-cert.pem",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE", "/mgmt-key.pem"
        ));
        initConfig();
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert", "/mgmt-cert.pem");
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.key", "/mgmt-key.pem");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path");
    }

    @Test
    public void managementInheritsPemWhenHttpHasBothPemAndKeystore() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/http-cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/http-key.pem",
                "KC_HTTPS_KEY_STORE_FILE", "server.p12",
                "KC_HTTPS_KEY_STORE_PASSWORD", "pass"
        ));
        initConfig();
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert", "/http-cert.pem");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path");
    }

    @Test
    public void explicitManagementPemOverridesInheritedHttpPem() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_CERTIFICATE_FILE", "/http-cert.pem",
                "KC_HTTPS_CERTIFICATE_KEY_FILE", "/http-key.pem",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_FILE", "/mgmt-cert.pem",
                "KC_HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE", "/mgmt-key.pem"
        ));
        initConfig();
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert", "/mgmt-cert.pem");
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.key", "/mgmt-key.pem");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path");
    }

    @Test
    public void explicitManagementKeystoreOverridesInheritedHttpKeystore() {
        makeInterfaceOccupied();
        putEnvVars(Map.of(
                "KC_HTTPS_KEY_STORE_FILE", "server.p12",
                "KC_HTTPS_KEY_STORE_PASSWORD", "http-pass",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_FILE", "mgmt.jks",
                "KC_HTTPS_MANAGEMENT_KEY_STORE_PASSWORD", "mgmt-pass"
        ));
        initConfig();
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.jks.path", "mgmt.jks",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.jks.password", "mgmt-pass"
        ));
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path");
    }
}
