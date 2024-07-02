/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.crls.infinispan;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.crls.CrlEntry;
import org.keycloak.crls.CrlLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.truststore.TruststoreProvider;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.keycloak.crls.infinispan.InfinispanCrlStorageProviderFactory.CacheExpirationMode.NEVER_EXPIRE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InfinispanCrlStorageProviderTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    private static Cache<String, CrlEntry> cache;

    @BeforeClass
    public static void setup() {
        System.setProperty("jboss.server.config.dir", "src/test/resources/crls/");
    }

    @Test
    public void foundCrlForIssuer() throws GeneralSecurityException, IOException {
        KeycloakSession keycloakSession = mock(KeycloakSession.class);
        TruststoreProvider truststoreProvider = mock(TruststoreProvider.class);
        when(keycloakSession.getProvider(TruststoreProvider.class)).thenReturn(truststoreProvider);
        KeyStore keyStore = mock(KeyStore.class);
        when(truststoreProvider.getTruststore()).thenReturn(keyStore);
        Map<X500Principal, X509Certificate> rootCerts = new HashMap<>();
        Map<X500Principal, X509Certificate> imCerts = new HashMap<>();
        rootCerts.put(getCaCert().getSubjectX500Principal(), getCaCert());
        when(truststoreProvider.getRootCertificates()).thenReturn(rootCerts);
        when(truststoreProvider.getIntermediateCertificates()).thenReturn(imCerts);
        InfinispanCrlStorageProvider infinispanCrlStorageProvider = new InfinispanCrlStorageProvider(keycloakSession, getCrlCache(), new HashMap<>(), new CrlLoader(keycloakSession), NEVER_EXPIRE, 1000);
        CrlEntry crlEntry = infinispanCrlStorageProvider.get(Collections.singletonList("revoked.crl"), getCaCert().getSubjectX500Principal());
        assertEquals("issuer matches", getCaCert().getSubjectX500Principal(), crlEntry.getIssuerCertificate().getSubjectX500Principal());
        assertEquals("has revoked certs", 1, crlEntry.getRevokedCerts().values().size());
    }

    @Test
    public void noCrlForIssuer() throws GeneralSecurityException, IOException {
        KeycloakSession keycloakSession = mock(KeycloakSession.class);
        TruststoreProvider truststoreProvider = mock(TruststoreProvider.class);
        when(keycloakSession.getProvider(TruststoreProvider.class)).thenReturn(truststoreProvider);
        KeyStore keyStore = mock(KeyStore.class);
        when(truststoreProvider.getTruststore()).thenReturn(keyStore);
        Map<X500Principal, X509Certificate> rootCerts = new HashMap<>();
        Map<X500Principal, X509Certificate> imCerts = new HashMap<>();
        rootCerts.put(getCaCert().getSubjectX500Principal(), getCaCert());
        when(truststoreProvider.getRootCertificates()).thenReturn(rootCerts);
        when(truststoreProvider.getIntermediateCertificates()).thenReturn(imCerts);
        InfinispanCrlStorageProvider infinispanCrlStorageProvider = new InfinispanCrlStorageProvider(keycloakSession, getCrlCache(), new HashMap<>(), new CrlLoader(keycloakSession), NEVER_EXPIRE, 1000);
        CrlEntry crlEntry = infinispanCrlStorageProvider.get(Collections.singletonList("revoked.crl"), getBadCaCert().getSubjectX500Principal());
        assertNull("has no crl entry", crlEntry);
    }

    private X509Certificate getCaCert() throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certificateFactory.generateCertificate(Files.newInputStream(Paths.get("src/test/resources/crls/ca.crt")));
    }

    private X509Certificate getBadCaCert() throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certificateFactory.generateCertificate(Files.newInputStream(Paths.get("src/test/resources/crls/test.crt")));
    }

    protected static Cache<String, CrlEntry> getCrlCache() {
        if (cache == null) {
            GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();
            gcb.jmx().domain(InfinispanConnectionProvider.JMX_DOMAIN + "-crl").enable();
            final DefaultCacheManager cacheManager = new DefaultCacheManager(gcb.build());

            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.jmxStatistics().enabled(true);
            Configuration cfg = cb.build();
            cacheManager.defineConfiguration(InfinispanConnectionProvider.CRLS_CACHE_NAME, cfg);

            cache = cacheManager.getCache(InfinispanConnectionProvider.CRLS_CACHE_NAME);
        }
        return cache;
    }
}
