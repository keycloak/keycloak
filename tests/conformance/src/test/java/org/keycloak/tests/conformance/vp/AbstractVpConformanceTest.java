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

package org.keycloak.tests.conformance.vp;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.conformance.AbstractConformanceTest;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;
import org.keycloak.tests.conformance.runner.ModuleRun;

import com.fasterxml.jackson.databind.JsonNode;

abstract class AbstractVpConformanceTest extends AbstractConformanceTest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Override
    protected JsonNode suiteConfig(ConformanceModuleVariant moduleVariant) {
        return VpSuiteConfig.create(
                        "keycloak-oid4vp",
                        VpVerifierKey.clientId(),
                        VpVerifierKey.caCertificatePem(),
                        VpVerifierKey.privateJwk())
                .toJson();
    }

    @Override
    protected Consumer<ModuleRun> interaction(ConformanceModuleVariant moduleVariant) {
        return moduleRun -> new KeycloakVerifierBrowser(suite, keycloakUrls.getBase(), browserSslContext())
                .login(VpConformanceRealmConfig.REALM, VpConformanceRealmConfig.CLIENT_ID,
                        VpConformanceRealmConfig.IDP_ALIAS, moduleRun);
    }

    // The browser visits both Keycloak and the suite over TLS, so it trusts the Keycloak server
    // certificate and the suite's nginx certificate.
    private SSLContext browserSslContext() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("conformance-suite", suite.nginxCertificate());

            KeyStore serverKeyStore = KeyStore.getInstance(certificates.getKeystoreFormat().name());
            try (InputStream in = Files.newInputStream(Path.of(certificates.getServerKeyStorePath()))) {
                serverKeyStore.load(in, certificates.getServerKeyStorePassword().toCharArray());
            }
            for (String alias : Collections.list(serverKeyStore.aliases())) {
                Certificate certificate = serverKeyStore.getCertificate(alias);
                if (certificate instanceof X509Certificate) {
                    trustStore.setCertificateEntry("keycloak-" + alias, certificate);
                }
            }

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build the verifier browser trust store", e);
        }
    }
}
