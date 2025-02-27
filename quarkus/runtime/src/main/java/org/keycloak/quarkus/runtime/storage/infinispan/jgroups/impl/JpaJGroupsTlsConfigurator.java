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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.jgroups.util.DefaultSocketFactory;
import org.jgroups.util.SocketFactory;
import org.keycloak.common.util.Retry;
import org.keycloak.config.CachingOptions;
import org.keycloak.infinispan.module.certificates.CertificateReloadManager;
import org.keycloak.infinispan.module.certificates.JGroupsCertificateHolder;
import org.keycloak.infinispan.module.configuration.global.KeycloakConfigurationBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;

import static org.keycloak.infinispan.module.certificates.CertificateReloadManager.CERTIFICATE_ID;
import static org.keycloak.infinispan.module.certificates.JGroupsCertificate.fromJson;
import static org.keycloak.quarkus.runtime.storage.infinispan.CacheManagerFactory.requiredIntegerProperty;

/**
 * JGroups mTLS configuration using certificates stored by {@link ServerConfigStorageProvider}.
 */
public class JpaJGroupsTlsConfigurator extends BaseJGroupsTlsConfigurator {

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
    SocketFactory createSocketFactory(ConfigurationBuilderHolder holder, KeycloakSession session) {
        var factory = session.getKeycloakSessionFactory();
        var kcConfig = holder.getGlobalConfigurationBuilder().addModule(KeycloakConfigurationBuilder.class);
        kcConfig.setKeycloakSessionFactory(factory);
        kcConfig.setJGroupsCertificateRotation(requiredIntegerProperty(CachingOptions.CACHE_EMBEDDED_MTLS_ROTATION));
        return Retry.call(iteration -> {
            try {
                var crtHolder = KeycloakModelUtils.runJobInTransactionWithResult(factory, this::createSocketFactoryInTransaction);
                var sslContext = SSLContext.getInstance(TLS_PROTOCOL);
                sslContext.init(new KeyManager[]{crtHolder.keyManager()}, new TrustManager[]{crtHolder.trustManager()}, null);
                var sf = createFromContext(sslContext);
                kcConfig.setJGroupCertificateHolder(crtHolder);
                return sf;
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }, STARTUP_RETRIES, STARTUP_RETRY_SLEEP_MILLIS);
    }

    private JGroupsCertificateHolder createSocketFactoryInTransaction(KeycloakSession session) {
        try {
            var rotationDays = requiredIntegerProperty(CachingOptions.CACHE_EMBEDDED_MTLS_ROTATION);
            var storage = session.getProvider(ServerConfigStorageProvider.class);
            var data = fromJson(storage.loadOrCreate(CERTIFICATE_ID, () -> CertificateReloadManager.generateSelfSignedCertificate(TimeUnit.DAYS.toSeconds(rotationDays) * 2L)));
            return JGroupsCertificateHolder.create(data);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static SocketFactory createFromContext(SSLContext context) {
        DefaultSocketFactory socketFactory = new DefaultSocketFactory(context);
        final SSLParameters serverParameters = new SSLParameters();
        serverParameters.setProtocols(new String[]{TLS_PROTOCOL_VERSION});
        serverParameters.setNeedClientAuth(true);
        socketFactory.setServerSocketConfigurator(socket -> ((SSLServerSocket) socket).setSSLParameters(serverParameters));
        return socketFactory;
    }

}
