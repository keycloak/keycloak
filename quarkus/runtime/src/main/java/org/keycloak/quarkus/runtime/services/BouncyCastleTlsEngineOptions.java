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

package org.keycloak.quarkus.runtime.services;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import java.util.Set;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.spi.tls.SslContextFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

/** Selects BC for this listener, without changing the JVM's provider order. */
public final class BouncyCastleTlsEngineOptions extends JdkSSLEngineOptions {
    private static final String[] SIGNATURE_SCHEMES = {
            "mldsa65", "mldsa44", "mldsa87", "ed25519", "ed448",
            "ecdsa_secp256r1_sha256", "ecdsa_secp384r1_sha384", "ecdsa_secp521r1_sha512",
            "rsa_pss_rsae_sha256", "rsa_pss_rsae_sha384", "rsa_pss_rsae_sha512",
            "rsa_pss_pss_sha256", "rsa_pss_pss_sha384", "rsa_pss_pss_sha512",
            "rsa_pkcs1_sha256", "rsa_pkcs1_sha384", "rsa_pkcs1_sha512"
    };
    private final Provider crypto = new BouncyCastleProvider();
    private final Provider tls = new BouncyCastleJsseProvider(crypto);

    @Override
    public BouncyCastleTlsEngineOptions copy() {
        return this;
    }

    @Override
    public SslContextFactory sslContextFactory() {
        return new Factory();
    }

    private final class Factory implements SslContextFactory {
        private boolean client;
        private boolean alpn;
        private ClientAuth clientAuth = ClientAuth.NONE;
        private KeyManagerFactory keys;
        private TrustManagerFactory trust;
        private Set<String> ciphers;
        private List<String> protocols;

        @Override
        public SslContextFactory forClient(boolean value) {
            client = value;
            return this;
        }

        @Override
        public SslContextFactory useAlpn(boolean value) {
            alpn = value;
            return this;
        }

        @Override
        public SslContextFactory clientAuth(ClientAuth value) {
            clientAuth = value;
            return this;
        }

        @Override
        public SslContextFactory keyMananagerFactory(KeyManagerFactory value) {
            keys = value;
            return this;
        }

        @Override
        public SslContextFactory trustManagerFactory(TrustManagerFactory value) {
            trust = value;
            return this;
        }

        @Override
        public SslContextFactory enabledCipherSuites(Set<String> value) {
            ciphers = value;
            return this;
        }

        @Override
        public SslContextFactory applicationProtocols(List<String> value) {
            protocols = value;
            return this;
        }

        @Override
        public SslContext create() throws SSLException {
            X509KeyManager manager = null;
            if (keys != null) {
                for (var key : keys.getKeyManagers()) {
                    if (key instanceof X509KeyManager x509) {
                        manager = importKeyManager(x509, client);
                        break;
                    }
                }
            }
            var builder = client ? SslContextBuilder.forClient() : SslContextBuilder.forServer(manager);
            if (client && manager != null) {
                builder.keyManager(manager);
            }
            builder.sslProvider(SslProvider.JDK).sslContextProvider(tls).clientAuth(clientAuth);
            if (trust != null) {
                builder.trustManager(trust);
            }
            if (ciphers != null && !ciphers.isEmpty()) {
                builder.ciphers(ciphers);
            }
            if (alpn && protocols != null && !protocols.isEmpty()) {
                builder.applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.FATAL_ALERT,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.FATAL_ALERT, protocols));
            }
            var context = builder.build();
            // BC keeps standalone PQ signatures disabled by default. Apply the policy
            // after Netty creates/wraps each engine, including engines created on reload.
            return new SslContext() {
                @Override
                public boolean isClient() {
                    return context.isClient();
                }

                @Override
                public List<String> cipherSuites() {
                    return context.cipherSuites();
                }

                @Override
                public io.netty.handler.ssl.ApplicationProtocolNegotiator applicationProtocolNegotiator() {
                    return context.applicationProtocolNegotiator();
                }

                @Override
                public javax.net.ssl.SSLSessionContext sessionContext() {
                    return context.sessionContext();
                }

                @Override
                public SSLEngine newEngine(io.netty.buffer.ByteBufAllocator allocator) {
                    return configure(context.newEngine(allocator));
                }

                @Override
                public SSLEngine newEngine(io.netty.buffer.ByteBufAllocator allocator, String host, int port) {
                    return configure(context.newEngine(allocator, host, port));
                }
                private SSLEngine configure(SSLEngine engine) {
                    var parameters = engine.getSSLParameters();
                    try {
                        javax.net.ssl.SSLParameters.class.getMethod("setSignatureSchemes", String[].class)
                                .invoke(parameters, (Object) SIGNATURE_SCHEMES.clone());
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("HTTPS BC TLS requires Java 21 or later", e);
                    }
                    engine.setSSLParameters(parameters);
                    return engine;
                }
            };
        }
    }

    /**
     * BC's key manager selects from all matching certificates using both SNI and
     * signature capabilities. Vert.x's per-host mapper otherwise retains just one
     * certificate when multiple identities have the same DNS name.
     */
    private X509KeyManager importKeyManager(X509KeyManager source, boolean client) throws SSLException {
        try {
            var aliases = new java.util.LinkedHashSet<String>();
            for (String type : new String[] {"RSA", "RSASSA-PSS", "EC", "DSA", "EdDSA", "Ed25519", "Ed448",
                    "ML-DSA", "ML-DSA-44", "ML-DSA-65", "ML-DSA-87"}) {
                var found = client ? source.getClientAliases(type, null) : source.getServerAliases(type, null);
                if (found != null) {
                    java.util.Collections.addAll(aliases, found);
                }
            }
            var store = java.security.KeyStore.getInstance("PKCS12", crypto);
            store.load(null, null);
            var password = new char[0];
            for (String alias : aliases) {
                var key = source.getPrivateKey(alias);
                var chain = source.getCertificateChain(alias);
                if (key != null && chain != null && chain.length != 0) {
                    store.setKeyEntry(alias, convertKey(key), password, convertChain(chain));
                }
            }
            var factory = KeyManagerFactory.getInstance("PKIX", tls);
            factory.init(store, password);
            for (var manager : factory.getKeyManagers()) {
                if (manager instanceof X509KeyManager x509) {
                    return x509;
                }
            }
            throw new SSLException("BC did not provide an HTTPS X509 key manager");
        } catch (Exception e) {
            throw new SSLException("Cannot import HTTPS software identities into BC", e);
        }
    }

    private PrivateKey convertKey(PrivateKey key) throws Exception {
        return key.getAlgorithm().startsWith("ML-DSA")
                ? KeyFactory.getInstance("ML-DSA", crypto).generatePrivate(new PKCS8EncodedKeySpec(key.getEncoded()))
                : key;
    }

    private X509Certificate[] convertChain(X509Certificate[] chain) throws Exception {
        if (!chain[0].getPublicKey().getAlgorithm().startsWith("ML-DSA")) {
            return chain;
        }
        var factory = java.security.cert.CertificateFactory.getInstance("X.509", crypto);
        var converted = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            converted[i] = (X509Certificate) factory.generateCertificate(
                    new java.io.ByteArrayInputStream(chain[i].getEncoded()));
        }
        return converted;
    }
}
