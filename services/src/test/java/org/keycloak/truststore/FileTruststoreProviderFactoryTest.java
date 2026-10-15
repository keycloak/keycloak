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

package org.keycloak.truststore;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.keycloak.Config;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.common.util.KeystoreUtil.TruststoreFormat;
import org.keycloak.config.HttpOptions;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.utils.ScopeUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class FileTruststoreProviderFactoryTest {

    private static final String[] SYSTEM_PROPERTY_KEYS = {
            TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY + ".orig"
    };

    private final Map<String, String> originalSystemProperties = new HashMap<>();
    private CryptoProvider originalCryptoProvider;
    private TruststoreProvider originalTruststoreProvider;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() {
        originalCryptoProvider = CryptoIntegration.isInitialised() ? CryptoIntegration.getProvider() : null;
        CryptoIntegration.setProvider(new DefaultCryptoProvider());
        originalTruststoreProvider = TruststoreProviderSingleton.get();
        for (String key : SYSTEM_PROPERTY_KEYS) {
            originalSystemProperties.put(key, System.getProperty(key));
        }
    }

    @After
    public void after() {
        for (String key : SYSTEM_PROPERTY_KEYS) {
            String value = originalSystemProperties.get(key);
            if (value == null) {
                System.getProperties().remove(key);
            } else {
                System.setProperty(key, value);
            }
        }
        TruststoreProviderSingleton.set(originalTruststoreProvider);
        CryptoIntegration.setProvider(originalCryptoProvider);
    }

    @Test
    public void testFallbackToSystemTruststore() throws IOException {
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(rootScope(new HashMap<>()));
        TruststoreProvider provider = factory.create(null);
        assertNotNull(provider.getTruststore());
        assertEquals(HostnameVerificationPolicy.DEFAULT, provider.getPolicy());
    }

    @Test
    public void testFallbackToSystemTruststoreWithHostnameVerification() throws IOException {
        Map<String, String> values = new HashMap<>();
        values.put(FileTruststoreProviderFactory.HOSTNAME_VERIFICATION_POLICY,
                HostnameVerificationPolicy.ANY.name());
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(ScopeUtil.createScope(values));
        TruststoreProvider provider = factory.create(null);
        assertNotNull(provider.getTruststore());
        assertEquals(HostnameVerificationPolicy.ANY, provider.getPolicy());
    }

    @Test
    public void testLoadGeneratedBcfksSystemTruststore() throws Exception {
        // given
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");

        // when
        TruststoreBuilder.setSystemTruststore(new String[] { url.getPath() }, false,
                temporaryFolder.getRoot().getAbsolutePath(), TruststoreFormat.BCFKS);

        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(ScopeUtil.createScope(new HashMap<>()));
        TruststoreProvider provider = factory.create(null);

        // then
        assertThat(certificates(provider.getTruststore()), equalTo(certificates(url)));
        assertFalse(provider.getRootCertificates().isEmpty());
        assertThat(provider.getTruststore().getProvider().getName(), equalTo("BC"));
    }

    @Test
    public void testLoadGeneratedPkcs12SystemTruststoreWithConfiguredCryptoProvider() throws Exception {
        // given
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");

        // when
        TruststoreBuilder.setSystemTruststore(new String[] { url.getPath() }, false,
                temporaryFolder.getRoot().getAbsolutePath());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", "SunJSSE");
        trustManagerFactory.init((KeyStore) null);

        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(ScopeUtil.createScope(new HashMap<>()));
        TruststoreProvider provider = factory.create(null);

        // then
        assertThat(System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY), equalTo(TruststoreFormat.PKCS12.name()));
        assertThat(System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY), equalTo("BC"));
        assertThat(provider.getTruststore().getProvider().getName(), equalTo("BC"));
        assertThat(certificates(provider.getTruststore()), equalTo(certificates(url)));
        assertThat(trustManagerCertificates(trustManagerFactory), equalTo(certificates(url)));
    }

    @Test
    public void testExplicitFileSettingDoesNotUseGeneratedStoreProvider() throws Exception {
        // given
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        TruststoreBuilder.setSystemTruststore(new String[] { url.getPath() }, false,
                temporaryFolder.getRoot().getAbsolutePath());
        String generatedPath = System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY);

        // when
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(rootScope(Map.of(
                "file", generatedPath,
                "password", TruststoreBuilder.DUMMY_PASSWORD,
                "type", TruststoreFormat.PKCS12.name())));

        // then
        TruststoreProvider provider = factory.create(null);
        assertThat(provider.getTruststore().getProvider().getName(),
                equalTo(KeyStore.getInstance(TruststoreFormat.PKCS12.name()).getProvider().getName()));
        assertThat(certificates(provider.getTruststore()), equalTo(certificates(url)));
    }

    @Test
    public void testExternalSystemTruststoreUsesConfiguredProvider() throws Exception {
        // given
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        setExternalSystemTruststore(url, "BC");

        // when
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(ScopeUtil.createScope(new HashMap<>()));

        // then
        TruststoreProvider provider = factory.create(null);
        assertThat(provider.getTruststore().getProvider().getName(), equalTo("BC"));
        assertThat(certificates(provider.getTruststore()), equalTo(certificates(url)));
    }

    @Test
    public void testExternalSystemTruststoreWithEmptyProviderUsesDefaultProvider() throws Exception {
        // given
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        setExternalSystemTruststore(url, "");

        // when
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(rootScope(new HashMap<>()));

        // then
        TruststoreProvider provider = factory.create(null);
        assertThat(provider.getTruststore().getProvider().getName(),
                equalTo(KeyStore.getInstance(TruststoreFormat.PKCS12.name()).getProvider().getName()));
        assertThat(certificates(provider.getTruststore()), equalTo(certificates(url)));
    }

    @Test
    public void testExternalSystemTruststoreWithoutProviderUsesDefaultProvider() throws Exception {
        // given
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        setExternalSystemTruststore(url, null);

        // when
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(rootScope(new HashMap<>()));

        // then
        TruststoreProvider provider = factory.create(null);
        assertThat(provider.getTruststore().getProvider().getName(),
                equalTo(KeyStore.getInstance(TruststoreFormat.PKCS12.name()).getProvider().getName()));
        assertThat(certificates(provider.getTruststore()), equalTo(certificates(url)));
    }

    @Test
    public void testInvalidSystemTruststoreProviderFailsClosed() throws Exception {
        // given
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        setExternalSystemTruststore(url, "missing-provider");

        // when / then
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> factory.init(rootScope(new HashMap<>())));
        assertThat(exception.getCause(), instanceOf(NoSuchProviderException.class));
    }

    private void setExternalSystemTruststore(URL certificates, String providerName) throws Exception {
        KeyStore generated = TruststoreBuilder.createMergedTruststore(new String[] { certificates.getPath() }, false);
        File external = TruststoreBuilder.saveTruststore(generated, TruststoreFormat.PKCS12,
                temporaryFolder.getRoot().getAbsolutePath(), TruststoreBuilder.DUMMY_PASSWORD.toCharArray());
        System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY, external.getAbsolutePath());
        System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY, TruststoreBuilder.DUMMY_PASSWORD);
        System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY, TruststoreFormat.PKCS12.name());
        if (providerName == null) {
            System.clearProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY);
        } else {
            System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY, providerName);
        }
    }

    private static Set<String> certificates(KeyStore truststore) throws Exception {
        Set<String> certificates = new HashSet<>();
        for (String alias : Collections.list(truststore.aliases())) {
            certificates.add(Base64.getEncoder().encodeToString(truststore.getCertificate(alias).getEncoded()));
        }
        return certificates;
    }

    private static Set<String> certificates(URL url) throws Exception {
        try (var input = url.openStream()) {
            Set<String> certificates = new HashSet<>();
            for (Certificate certificate : CertificateFactory.getInstance("X.509").generateCertificates(input)) {
                certificates.add(Base64.getEncoder().encodeToString(certificate.getEncoded()));
            }
            return certificates;
        }
    }

    private static Set<String> trustManagerCertificates(TrustManagerFactory trustManagerFactory) throws Exception {
        Set<String> certificates = new HashSet<>();
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                for (X509Certificate certificate : x509TrustManager.getAcceptedIssuers()) {
                    certificates.add(Base64.getEncoder().encodeToString(certificate.getEncoded()));
                }
            }
        }
        return certificates;
    }

    @Test
    public void testLoadPemHttpsTruststoreUsesPreferredTruststoreType() throws Exception {
        CryptoIntegration.setProvider(new DefaultCryptoProvider() {
            @Override
            public TruststoreFormat getPreferredGeneratedTrustStoreType() {
                return TruststoreFormat.BCFKS;
            }
        });
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");

        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(rootScope(Map.of(HttpOptions.HTTPS_TRUST_STORE_FILE.getKey(), url.getPath())));
        TruststoreProvider provider = factory.create(null);

        assertEquals(TruststoreFormat.BCFKS.name(), provider.getHttpsTruststore().getType());
        assertEquals(2, Collections.list(provider.getHttpsTruststore().aliases()).size());
    }

    private static Config.Scope rootScope(Map<String, String> values) {
        return new Config.AbstractScope() {
            @Override
            public String get(String key) {
                return values.get(key);
            }

            @Override
            public Config.Scope scope(String... scope) {
                return this;
            }

            @Override
            public Set<String> getPropertyNames() {
                return values.keySet();
            }

            @Override
            public Config.Scope root() {
                return this;
            }
        };
    }

}
