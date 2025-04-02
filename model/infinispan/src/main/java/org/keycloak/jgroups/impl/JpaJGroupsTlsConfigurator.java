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

package org.keycloak.jgroups.impl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jgroups.util.DefaultSocketFactory;
import org.jgroups.util.SocketFactory;
import org.keycloak.infinispan.module.configuration.global.KeycloakConfigurationBuilder;
import org.keycloak.jgroups.JGroupsConfigurator;
import org.keycloak.jgroups.JGroupsStackConfigurator;
import org.keycloak.jgroups.JGroupsUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.spi.infinispan.JGroupsCertificateProvider;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;

/**
 * JGroups mTLS configuration using certificates stored by {@link ServerConfigStorageProvider}.
 */
public class JpaJGroupsTlsConfigurator implements JGroupsStackConfigurator {

    private static final String TLS_PROTOCOL_VERSION = "TLSv1.3";
    private static final String TLS_PROTOCOL = "TLS";
    public static final JpaJGroupsTlsConfigurator INSTANCE = new JpaJGroupsTlsConfigurator();


    @Override
    public void configure(ConfigurationBuilderHolder holder, KeycloakSession session) {
        var kcConfig = holder.getGlobalConfigurationBuilder().addModule(KeycloakConfigurationBuilder.class);
        kcConfig.setKeycloakSessionFactory(session.getKeycloakSessionFactory());
        var provider = session.getProvider(JGroupsCertificateProvider.class);
        if (provider == null || !provider.isEnabled()) {
            return;
        }
        var factory = createSocketFactory(provider);
        JGroupsUtil.transportOf(holder).addProperty(JGroupsTransport.SOCKET_FACTORY, factory);
        JGroupsUtil.validateTlsAvailable(holder);
        JGroupsConfigurator.logger.info("JGroups Encryption enabled (mTLS).");
    }


    private static SocketFactory createSocketFactory(JGroupsCertificateProvider provider) {
        try {
            var sslContext = SSLContext.getInstance(TLS_PROTOCOL);
            sslContext.init(new KeyManager[]{provider.keyManager()}, new TrustManager[]{provider.trustManager()}, null);
            return createFromContext(sslContext);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            // we should have valid certificates and keys.
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
