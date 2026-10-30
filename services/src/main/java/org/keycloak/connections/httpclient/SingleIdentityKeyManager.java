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

package org.keycloak.connections.httpclient;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

/**
 * Key manager that always presents one client certificate whatever issuers the server lists.
 */
public final class SingleIdentityKeyManager extends X509ExtendedKeyManager {

    private static final String ALIAS = "client-certificate";

    private final PrivateKey privateKey;
    private final X509Certificate[] certificateChain;

    public SingleIdentityKeyManager(PrivateKey privateKey, X509Certificate[] certificateChain) {
        if (privateKey == null) {
            throw new IllegalArgumentException("The private key is required");
        }
        if (certificateChain == null || certificateChain.length == 0) {
            throw new IllegalArgumentException("The certificate chain is required");
        }
        this.privateKey = privateKey;
        this.certificateChain = certificateChain.clone();
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return new String[] { ALIAS };
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return ALIAS;
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return ALIAS;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return null;
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return null;
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return ALIAS.equals(alias) ? certificateChain.clone() : null;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return ALIAS.equals(alias) ? privateKey : null;
    }
}
