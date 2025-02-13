/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jgroups.util.DefaultSocketFactory;
import org.jgroups.util.SocketFactory;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.Retry;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;
import org.keycloak.util.JsonSerialization;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 * JGroups mTLS configuration using certificates stored by {@link ServerConfigStorageProvider}.
 */
public class JpaJGroupsTlsConfigurator extends BaseJGroupsTlsConfigurator {

    private static final char[] KEY_PASSWORD = "jgroups-password".toCharArray();
    private static final String CERTIFICATE_ID = "crt_jgroups";
    private static final String KEYSTORE_ALIAS = "jgroups";
    private static final String JGROUPS_SUBJECT = "jgroups";
    private static final String TLS_PROTOCOL_VERSION = "TLSv1.3";
    private static final String TLS_PROTOCOL = "TLS";
    private static final int STARTUP_RETRIES = 2;
    private static final int STARTUP_RETRY_SLEEP_MILLIS = 10;
    public static final JpaJGroupsTlsConfigurator INSTANCE = new JpaJGroupsTlsConfigurator();

    @Override
    public boolean requiresKeycloakSession() {
        return true;
    }

    @Override
    SocketFactory createSocketFactory(KeycloakSession session) {
        var factory = session.getKeycloakSessionFactory();
        return Retry.call(iteration -> KeycloakModelUtils.runJobInTransactionWithResult(factory, this::createSocketFactoryInTransaction), STARTUP_RETRIES, STARTUP_RETRY_SLEEP_MILLIS);
    }

    private SocketFactory createSocketFactoryInTransaction(KeycloakSession session) {
        try {
            var storage = session.getProvider(ServerConfigStorageProvider.class);
            var data = fromJson(storage.loadOrCreate(CERTIFICATE_ID, JpaJGroupsTlsConfigurator::generateSelfSignedCertificate));
            var km = createKeyManager(data.getKeyPair(), data.getCertificate());
            var tm = createTrustManager(data.getCertificate());
            var sslContext = SSLContext.getInstance(TLS_PROTOCOL);
            sslContext.init(new KeyManager[]{km}, new TrustManager[]{tm}, null);
            return createFromContext(sslContext);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private X509ExtendedKeyManager createKeyManager(KeyPair keyPair, X509Certificate certificate) throws GeneralSecurityException, IOException {
        var ks = CryptoIntegration.getProvider().getKeyStore(KeystoreUtil.KeystoreFormat.JKS);
        ks.load(null, null);
        ks.setKeyEntry(KEYSTORE_ALIAS, keyPair.getPrivate(), KEY_PASSWORD, new java.security.cert.Certificate[]{certificate});
        var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, KEY_PASSWORD);
        for (KeyManager km : kmf.getKeyManagers()) {
            if (km instanceof X509ExtendedKeyManager) {
                return (X509ExtendedKeyManager) km;
            }
        }
        throw new GeneralSecurityException("Could not obtain an X509ExtendedKeyManager");
    }

    private X509ExtendedTrustManager createTrustManager(X509Certificate certificate) throws GeneralSecurityException, IOException {
        var ks = CryptoIntegration.getProvider().getKeyStore(KeystoreUtil.KeystoreFormat.JKS);
        ks.load(null, null);
        ks.setCertificateEntry(KEYSTORE_ALIAS, certificate);
        var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509ExtendedTrustManager) {
                return (X509ExtendedTrustManager) tm;
            }
        }
        throw new GeneralSecurityException("Could not obtain an X509TrustManager");
    }

    private static String generateSelfSignedCertificate() {
        var keyPair = KeyUtils.generateRsaKeyPair(2048);
        var certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, JGROUPS_SUBJECT);
        var entity = new CertificateEntity();
        entity.setCertificate(certificate);
        entity.setKeyPair(keyPair);
        return toJson(entity);
    }

    private static SocketFactory createFromContext(SSLContext context) {
        DefaultSocketFactory socketFactory = new DefaultSocketFactory(context);
        final SSLParameters serverParameters = new SSLParameters();
        serverParameters.setProtocols(new String[]{TLS_PROTOCOL_VERSION});
        serverParameters.setNeedClientAuth(true);
        socketFactory.setServerSocketConfigurator(socket -> ((SSLServerSocket) socket).setSSLParameters(serverParameters));
        return socketFactory;
    }

    private static String toJson(CertificateEntity entity) {
        try {
            return JsonSerialization.mapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Should never happen!", e);
        }
    }

    private static CertificateEntity fromJson(String json) {
        try {
            return JsonSerialization.mapper.readValue(json, CertificateEntity.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Should never happen!", e);
        }
    }

}
