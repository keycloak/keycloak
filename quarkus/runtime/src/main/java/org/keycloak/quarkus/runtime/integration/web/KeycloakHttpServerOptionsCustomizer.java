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

package org.keycloak.quarkus.runtime.integration.web;

import static org.eclipse.microprofile.config.inject.ConfigProperty.UNCONFIGURED_VALUE;

import java.security.KeyStore;
import java.util.Optional;
import java.util.function.Function;
import javax.enterprise.context.ApplicationScoped;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.impl.KeyStoreHelper;

@ApplicationScoped
public class KeycloakHttpServerOptionsCustomizer implements HttpServerOptionsCustomizer {

    private static final String PKCS11_KEY_STORE_TYPE = "PKCS11";

    @ConfigProperty(name = "kc.https-key-store-type", defaultValue = "none")
    String httpsKeyStoreType;

    @ConfigProperty(name = "kc.https-key-store-password")
    String httpsKeyStorePassword;

    private KeyStoreHelper keyStoreHelper;

    @Override
    public void customizeHttpsServer(HttpServerOptions options) {
        if (PKCS11_KEY_STORE_TYPE.equalsIgnoreCase(httpsKeyStoreType)) {
            try {
                keyStoreHelper = new KeyStoreHelper(loadPkcs11KeyStore(), httpsKeyStorePassword, null);
                options.setKeyStoreOptions(createPkcs11KeyStoreOptions(keyStoreHelper));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private KeyStore loadPkcs11KeyStore() {
        try {
            KeyStore ks = KeyStore.getInstance(httpsKeyStoreType);

            ks.load(null, Optional.ofNullable(httpsKeyStorePassword).map(String::toCharArray).orElse(null));

            return ks;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JksOptions createPkcs11KeyStoreOptions(KeyStoreHelper keyStoreHelper) {
        return new JksOptions() {
            @Override
            public JksOptions copy() {
                return this;
            }

            @Override
            public KeyManagerFactory getKeyManagerFactory(Vertx vertx) throws Exception {
                return keyStoreHelper.getKeyMgrFactory();
            }

            @Override
            public Function<String, X509KeyManager> keyManagerMapper(Vertx vertx) throws Exception {
                return keyStoreHelper::getKeyMgr;
            }
        };
    }
}
