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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class FileTruststoreProviderFactoryTest {

    private static final String[] SYSTEM_PROPERTY_KEYS = {
            TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY + ".orig"
    };

    private final Map<String, String> originalSystemProperties = new HashMap<>();
    private CryptoProvider originalCryptoProvider;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() {
        originalCryptoProvider = CryptoIntegration.isInitialised() ? CryptoIntegration.getProvider() : null;
        CryptoIntegration.setProvider(new DefaultCryptoProvider());
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
        CryptoIntegration.setProvider(originalCryptoProvider);
    }

    @Test
    public void testFallbackToSystemTruststore() throws IOException {
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(ScopeUtil.createScope(new HashMap<>()));
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
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        KeyStore bcfksTruststore = TruststoreBuilder.createMergedTruststore(new String[] { url.getPath() }, false,
                TruststoreFormat.BCFKS);
        File saved = TruststoreBuilder.saveTruststore(bcfksTruststore, TruststoreFormat.BCFKS,
                temporaryFolder.getRoot().getAbsolutePath(), TruststoreBuilder.DUMMY_PASSWORD.toCharArray());

        System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY, saved.getAbsolutePath());
        System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY, TruststoreBuilder.DUMMY_PASSWORD);
        System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY, TruststoreFormat.BCFKS.name());

        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(ScopeUtil.createScope(new HashMap<>()));
        TruststoreProvider provider = factory.create(null);

        assertEquals(2, Collections.list(provider.getTruststore().aliases()).size());
        assertFalse(provider.getRootCertificates().isEmpty());
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
