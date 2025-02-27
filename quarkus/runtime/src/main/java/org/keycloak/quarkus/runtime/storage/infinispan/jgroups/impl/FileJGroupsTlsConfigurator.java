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

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.jgroups.util.FileWatcher;
import org.jgroups.util.SocketFactory;
import org.jgroups.util.TLS;
import org.jgroups.util.TLSClientAuth;
import org.keycloak.models.KeycloakSession;

import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY;
import static org.keycloak.quarkus.runtime.storage.infinispan.CacheManagerFactory.requiredStringProperty;

/**
 * JGroups mTLS configuration using the provided KeyStore and TrustStore files.
 */
public class FileJGroupsTlsConfigurator extends BaseJGroupsTlsConfigurator {

    public static final FileJGroupsTlsConfigurator INSTANCE = new FileJGroupsTlsConfigurator();

    @Override
    SocketFactory createSocketFactory(ConfigurationBuilderHolder holder, KeycloakSession ignored) {
        var tls = new TLS()
                .enabled(true)
                .setKeystorePath(requiredStringProperty(CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY))
                .setTruststorePath(requiredStringProperty(CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY))
                .setKeystorePassword(requiredStringProperty(CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY))
                .setTruststorePassword(requiredStringProperty(CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY))
                .setKeystoreType("pkcs12")
                .setTruststoreType("pkcs12")
                .setClientAuth(TLSClientAuth.NEED)
                .setProtocols(new String[]{"TLSv1.3"});
        // listen to file changes and reloads the key and trust stores.
        tls.setWatcher(new FileWatcher());
        return tls.createSocketFactory();
    }

    @Override
    public boolean requiresKeycloakSession() {
        return false;
    }
}
