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
import org.keycloak.quarkus.runtime.configuration.AbstractConfigurationTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TlsStoreDispatchTest extends AbstractConfigurationTest {

    @Test
    public void detectPkcs12FromExtension() {
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.p12", null), is(HttpPropertyMappers.StoreType.PKCS12));
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.pfx", null), is(HttpPropertyMappers.StoreType.PKCS12));
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.pkcs12", null), is(HttpPropertyMappers.StoreType.PKCS12));
    }

    @Test
    public void detectJksFromExtension() {
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.jks", null), is(HttpPropertyMappers.StoreType.JKS));
    }

    @Test
    public void detectBcfksFromExtension() {
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.bcfks", null), is(HttpPropertyMappers.StoreType.OTHER));
    }

    @Test
    public void explicitTypeOverridesExtension() {
        assertThat(HttpPropertyMappers.detectStoreType("JKS", "server.p12", null), is(HttpPropertyMappers.StoreType.JKS));
        assertThat(HttpPropertyMappers.detectStoreType("PKCS12", "server.jks", null), is(HttpPropertyMappers.StoreType.PKCS12));
        assertThat(HttpPropertyMappers.detectStoreType("BCFKS", "server.p12", null), is(HttpPropertyMappers.StoreType.OTHER));
    }

    @Test
    public void keystoreExtensionDetectedAsJks() {
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.keystore", HttpPropertyMappers.StoreRole.KEY_STORE),
                is(HttpPropertyMappers.StoreType.JKS));
    }

    @Test
    public void truststoreExtensionDetectedAsJks() {
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.truststore", HttpPropertyMappers.StoreRole.TRUST_STORE),
                is(HttpPropertyMappers.StoreType.JKS));
    }

    @Test
    public void keystoreExtensionNotValidForTrustStore() {
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.keystore", HttpPropertyMappers.StoreRole.TRUST_STORE) == null,
                is(true));
    }

    @Test
    public void truststoreExtensionNotValidForKeyStore() {
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.truststore", HttpPropertyMappers.StoreRole.KEY_STORE) == null,
                is(true));
    }

    @Test
    public void unrecognizedExtensionReturnsNull() {
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.ks", null) == null, is(true));
        assertThat(HttpPropertyMappers.detectStoreType(null, "server.xyz", null) == null, is(true));
    }

    @Test
    public void filterOtherStoreTypeReturnsNullForKnownTypes() {
        assertThat(HttpPropertyMappers.filterOtherStoreType("PKCS12", HttpOptions.HTTPS_KEY_STORE_FILE) == null, is(true));
        assertThat(HttpPropertyMappers.filterOtherStoreType("P12", HttpOptions.HTTPS_KEY_STORE_FILE) == null, is(true));
        assertThat(HttpPropertyMappers.filterOtherStoreType("JKS", HttpOptions.HTTPS_KEY_STORE_FILE) == null, is(true));
        assertThat(HttpPropertyMappers.filterOtherStoreType("jks", HttpOptions.HTTPS_KEY_STORE_FILE) == null, is(true));
    }

    @Test
    public void filterOtherStoreTypeKeepsExoticTypesWhenFileSet() {
        createConfigFromCliArguments("--https-key-store-file=server.bcfks", "--https-key-store-type=BCFKS");
        assertThat(HttpPropertyMappers.filterOtherStoreType("BCFKS", HttpOptions.HTTPS_KEY_STORE_FILE), is("BCFKS"));
    }

    @Test
    public void filterOtherStoreTypeReturnsNullWithoutFile() {
        assertThat(HttpPropertyMappers.filterOtherStoreType("BCFKS", HttpOptions.HTTPS_KEY_STORE_FILE) == null, is(true));
    }

    @Test
    public void httpKeystorePasswordFollowsFileTypeBucket() {
        createConfigFromCliArguments("--https-key-store-file=server.jks", "--https-key-store-password=secret");
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "key-store.jks.path", "server.jks",
                HttpPropertyMappers.TLS_PREFIX + "key-store.jks.password", "secret"
        ));
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "key-store.p12.password");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "key-store.other.password");
    }

    @Test
    public void httpTrustStoreBcfksDispatch() {
        createConfigFromCliArguments("--https-trust-store-file=trust.bcfks",
                "--https-trust-store-type=BCFKS", "--https-trust-store-password=pass");
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "trust-store.other.path", "trust.bcfks",
                HttpPropertyMappers.TLS_PREFIX + "trust-store.other.password", "pass",
                HttpPropertyMappers.TLS_PREFIX + "trust-store.other.type", "BCFKS"
        ));
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "trust-store.p12.path");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "trust-store.jks.path");
    }

    @Test
    public void pemTrustStoreDetectedFromPemExtension() {
        createConfigFromCliArguments("--https-trust-store-file=ca-cert.pem");
        assertExternalConfig(HttpPropertyMappers.TLS_PREFIX + "trust-store.pem.certs", "ca-cert.pem");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "trust-store.p12.path");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "trust-store.jks.path");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "trust-store.other.path");
    }

    @Test
    public void pemTrustStoreDetectedFromCrtExtension() {
        createConfigFromCliArguments("--https-trust-store-file=ca-cert.crt");
        assertExternalConfig(HttpPropertyMappers.TLS_PREFIX + "trust-store.pem.certs", "ca-cert.crt");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "trust-store.p12.path");
    }

    @Test
    public void pemTrustStoreDetectedFromCaExtension() {
        createConfigFromCliArguments("--https-trust-store-file=ca-cert.ca");
        assertExternalConfig(HttpPropertyMappers.TLS_PREFIX + "trust-store.pem.certs", "ca-cert.ca");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "trust-store.p12.path");
    }

    @Test
    public void managementInheritsPemTrustStoreFromHttp() {
        putEnvVar("KC_HEALTH_ENABLED", "true");
        createConfigFromCliArguments("--https-trust-store-file=ca-cert.pem");
        assertExternalConfig(ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.pem.certs", "ca-cert.pem");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "trust-store.p12.path");
    }

    @Test
    public void pemKeyStoreWithPkcs12TrustStore() {
        createConfigFromCliArguments(
                "--https-certificate-file=/cert.pem", "--https-certificate-key-file=/key.pem",
                "--https-trust-store-file=truststore.p12", "--https-trust-store-password=pass");
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.cert", "/cert.pem",
                HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.key", "/key.pem",
                HttpPropertyMappers.TLS_PREFIX + "trust-store.p12.path", "truststore.p12",
                HttpPropertyMappers.TLS_PREFIX + "trust-store.p12.password", "pass"
        ));
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "key-store.p12.path");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "trust-store.pem.certs");
    }

    @Test
    public void pkcs12KeyStoreWithPemTrustStore() {
        createConfigFromCliArguments(
                "--https-key-store-file=server.p12", "--https-key-store-password=pass",
                "--https-trust-store-file=ca-cert.pem");
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "key-store.p12.path", "server.p12",
                HttpPropertyMappers.TLS_PREFIX + "key-store.p12.password", "pass",
                HttpPropertyMappers.TLS_PREFIX + "trust-store.pem.certs", "ca-cert.pem"
        ));
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.cert");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "trust-store.p12.path");
    }

    @Test
    public void pemTakesPrecedenceOverOtherKeyStore() {
        createConfigFromCliArguments(
                "--https-certificate-file=/cert.pem", "--https-certificate-key-file=/key.pem",
                "--https-key-store-file=server.bcfks", "--https-key-store-type=BCFKS", "--https-key-store-password=pass");
        assertExternalConfig(Map.of(
                HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.cert", "/cert.pem",
                HttpPropertyMappers.TLS_PREFIX + "key-store.pem.default.key", "/key.pem"
        ));
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "key-store.other.type");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "key-store.other.path");
        assertExternalConfigNull(HttpPropertyMappers.TLS_PREFIX + "key-store.other.password");
    }

    @Test
    public void managementPemTakesPrecedenceOverInheritedOtherKeyStore() {
        putEnvVar("KC_HEALTH_ENABLED", "true");
        createConfigFromCliArguments(
                "--https-certificate-file=/cert.pem", "--https-certificate-key-file=/key.pem",
                "--https-key-store-file=server.bcfks", "--https-key-store-type=BCFKS", "--https-key-store-password=pass");
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.cert", "/cert.pem",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.pem.default.key", "/key.pem"
        ));
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.other.type");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.other.path");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.other.password");
    }

    @Test
    public void managementInheritsKeystoreTypeFromHttp() {
        putEnvVar("KC_HEALTH_ENABLED", "true");
        createConfigFromCliArguments("--https-key-store-file=server.jks",
                "--https-key-store-password=pass", "--https-key-store-type=JKS");
        assertConfig("https-management-key-store-type", "JKS");
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.jks.path", "server.jks",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.jks.password", "pass"
        ));
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.other.type");
    }

    @Test
    public void managementInheritsPkcs12TypeFromHttp() {
        putEnvVar("KC_HEALTH_ENABLED", "true");
        createConfigFromCliArguments("--https-key-store-file=server.p12", "--https-key-store-password=pass");
        assertExternalConfig(Map.of(
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.path", "server.p12",
                ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.p12.password", "pass"
        ));
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.jks.path");
        assertExternalConfigNull(ManagementPropertyMappers.MGMT_TLS_PREFIX + "key-store.other.type");
    }
}
