/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.keystore;

import java.security.KeyStore;

import org.keycloak.provider.Provider;

/**
 * KeyStore provider provides credentials for clients and servers.
 */
public interface KeyStoreProvider extends Provider {

    public static final String LDAP_CLIENT_KEYSTORE = "ldap-client-keystore";

    /**
     * Loads KeyStore of given identifier.
     *
     * @param keyStoreIdentifier Identifier of the wanted KeyStore, such as LDAP_CLIENT_KEYSTORE.
     * @return KeyStore.
     */
    KeyStore loadKeyStore(String keyStoreIdentifier);

    /**
     * Loads KeyStore of given identifier and returns a KeyStore.Builder.
     * Builder encapsulates both KeyStore and KeyEntry password(s).
     *
     * @param keyStoreIdentifier Identifier of the wanted KeyStore, such as LDAP_CLIENT_KEYSTORE.
     * @return Builder for KeyStore.
     */

    KeyStore.Builder loadKeyStoreBuilder(String keyStoreIdentifier);

}
