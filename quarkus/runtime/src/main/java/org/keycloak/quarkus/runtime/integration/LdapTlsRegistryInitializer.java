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

package org.keycloak.quarkus.runtime.integration;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.keycloak.storage.ldap.idm.store.ldap.LDAPSSLSocketFactory;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import org.jboss.logging.Logger;

/**
 * Initializes LDAP socket factory with client certificate TLS configuration from the Quarkus TLS Registry.
 * If a TLS configuration named "ldap" is present, client certificates are used for SASL EXTERNAL authentication with LDAP.
 */
@ApplicationScoped
public class LdapTlsRegistryInitializer {

    private static final Logger logger = Logger.getLogger(LdapTlsRegistryInitializer.class);
    private static final String TLS_CONFIG_NAME = "ldap";

    void onStart(@Observes StartupEvent event, TlsConfigurationRegistry registry) {
        Optional<TlsConfiguration> tlsConfig = registry.get(TLS_CONFIG_NAME);
        if (tlsConfig.isPresent()) {
            TlsConfiguration config = tlsConfig.get();
            LDAPSSLSocketFactory.setSSLSocketFactorySupplier(() -> {
                try {
                    return config.createSSLContext().getSocketFactory();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create SSLContext from TLS Registry '" + TLS_CONFIG_NAME + "' configuration", e);
                }
            });
            logger.debug("Loaded LDAP client certificate configuration from Quarkus TLS Registry and configured socket factory for SASL EXTERNAL authentication");
        }
    }
}
