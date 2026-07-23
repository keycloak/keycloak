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

package org.keycloak.crypto.glassless;

import java.lang.reflect.InvocationTargetException;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;

import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.elytron.WildFlyElytronProvider;

import org.jboss.logging.Logger;

public class GlasslessCryptoProvider extends WildFlyElytronProvider {

    private static final Logger LOG = Logger.getLogger(GlasslessCryptoProvider.class);
    private static final String PROVIDER_CLASS = "net.glassless.provider.GlaSSLessProvider";
    private static final String FIPS_STATUS_CLASS = "net.glassless.provider.FIPSStatus";
    private static final String OPENSSL_CRYPTO_CLASS = "net.glassless.provider.internal.OpenSSLCrypto";
    private static final String PROVIDER_NAME = "GlaSSLess";

    private final Provider glasslessProvider;

    public GlasslessCryptoProvider() {
        this(false);
    }

    protected GlasslessCryptoProvider(boolean requireFips) {
        this(resolveProvider(), requireFips, null);
    }

    GlasslessCryptoProvider(Provider provider, boolean requireFips, GlasslessFipsStatus fipsStatus) {
        glasslessProvider = provider;
        boolean inserted = Security.getProvider(PROVIDER_NAME) == null;
        if (inserted) {
            Security.insertProviderAt(glasslessProvider, 1);
        }

        try {
            GlasslessFipsStatus resolvedFipsStatus = fipsStatus == null ? resolveFipsStatus(glasslessProvider) : fipsStatus;
            if (requireFips && !resolvedFipsStatus.isActive()) {
                throw new IllegalStateException("Glassless strict mode requires an active OpenSSL FIPS provider");
            }
            if (requireFips && Security.getProviders()[0] != glasslessProvider) {
                throw new IllegalStateException("Glassless must be the highest priority security provider in strict mode");
            }
            LOG.infof("GlasslessCryptoProvider created: KC(%s, FIPS mode: %s, FIPS provider available: %s, OpenSSL FIPS default properties: %s)",
                    glasslessProvider, resolvedFipsStatus.fipsMode() ? "enabled" : "disabled",
                    resolvedFipsStatus.fipsProviderAvailable(), resolvedFipsStatus.openSslFipsEnabled() ? "enabled" : "disabled");
            LOG.info(formatProviderConfiguration(glasslessProvider));
        } catch (RuntimeException cause) {
            if (inserted) {
                Security.removeProvider(PROVIDER_NAME);
            }
            throw cause;
        }
    }

    record GlasslessFipsStatus(boolean fipsMode, boolean fipsProviderAvailable, boolean openSslFipsEnabled) {

        boolean isActive() {
            return fipsMode && fipsProviderAvailable && openSslFipsEnabled;
        }
    }

    private static GlasslessFipsStatus resolveFipsStatus(Provider provider) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new GlasslessFipsStatus(
                invokeBoolean(provider, "isFIPSMode"),
                invokeStaticBoolean(classLoader, FIPS_STATUS_CLASS, "isFIPSProviderAvailable"),
                isOpenSslDefaultContextFipsEnabled(classLoader));
    }

    /*
     * Glassless 0.13.0 does not expose the OpenSSL default context FIPS property
     * through its public API. FIPSStatus.isFIPSEnabled() is not sufficient for
     * strict mode because it can also return true from glassless.fips.mode or
     * the operating system crypto policy, even when OpenSSL is not constrained
     * to the FIPS provider.
     *
     * Keep this temporary reflection in one function so it can be removed
     * without changing the strict mode validation flow. A future Glassless API
     * should expose a public, nonoverrideable method similar to:
     *
     *     FIPSStatus.isOpenSslDefaultContextFipsEnabled()
     *
     * The method should return the result of
     * EVP_default_properties_is_fips_enabled(NULL) directly. It must not honor
     * the Glassless system property or infer activation from the operating
     * system policy. A richer API could return an immutable status object with
     * separate values for configured policy, provider availability, and active
     * OpenSSL default properties. Once that API exists, only this function
     * should need to change.
     */
    static boolean isOpenSslDefaultContextFipsEnabled(ClassLoader classLoader) {
        return invokeStaticBoolean(classLoader, OPENSSL_CRYPTO_CLASS, "isFIPSEnabled");
    }

    static String formatProviderConfiguration(Provider provider) {
        Map<String, TreeSet<String>> servicesByType = new TreeMap<>();
        for (Provider.Service service : provider.getServices()) {
            servicesByType.computeIfAbsent(service.getType(), ignored -> new TreeSet<>()).add(service.getAlgorithm());
        }

        StringBuilder configuration = new StringBuilder("Glassless provider configuration:");
        servicesByType.forEach((type, algorithms) -> {
            configuration.append(System.lineSeparator())
                    .append(type).append(" (").append(algorithms.size()).append("):");
            algorithms.forEach(algorithm -> configuration.append(System.lineSeparator()).append("  ").append(algorithm));
        });
        return configuration.toString();
    }

    private static Provider resolveProvider() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Provider existingProvider = Security.getProvider(PROVIDER_NAME);
        return existingProvider == null ? createProvider(classLoader) : existingProvider;
    }

    @Override
    public Provider getBouncyCastleProvider() {
        return glasslessProvider;
    }

    @Override
    public ECParameterSpec createECParams(String curveName) {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", glasslessProvider);
            parameters.init(new ECGenParameterSpec(curveName));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (Exception cause) {
            throw new RuntimeException("Failed to generate EC parameter spec", cause);
        }
    }

    @Override
    public KeyPairGenerator getKeyPairGen(String algorithm) throws NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance(algorithm, glasslessProvider);
    }

    @Override
    public KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        if ("ECDSA".equals(algorithm)) {
            algorithm = "EC";
        }
        return KeyFactory.getInstance(algorithm, glasslessProvider);
    }

    @Override
    public Cipher getAesGcmCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("AES/GCM/NoPadding", glasslessProvider);
    }

    @Override
    public SecretKeyFactory getSecretKeyFact(String keyAlgorithm) throws NoSuchAlgorithmException {
        return SecretKeyFactory.getInstance(keyAlgorithm, glasslessProvider);
    }

    @Override
    public Signature getSignature(String sigAlgName) throws NoSuchAlgorithmException {
        return Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(sigAlgName), glasslessProvider);
    }

    private static Provider createProvider(ClassLoader classLoader) {
        try {
            Object provider = Class.forName(PROVIDER_CLASS, true, classLoader).getDeclaredConstructor().newInstance();
            if (provider instanceof Provider securityProvider) {
                return securityProvider;
            }
            throw new IllegalStateException(PROVIDER_CLASS + " is not a Java security provider");
        } catch (InvocationTargetException cause) {
            throw new IllegalStateException("Failed to initialize Glassless", cause.getCause());
        } catch (ReflectiveOperationException | LinkageError cause) {
            throw new IllegalStateException("Glassless 0.13.0 or later and Java 25 or later are required", cause);
        }
    }

    private static boolean invokeBoolean(Object target, String method) {
        try {
            return (boolean) target.getClass().getMethod(method).invoke(target);
        } catch (ReflectiveOperationException cause) {
            throw new IllegalStateException("Glassless 0.13.0 or later is required", cause);
        }
    }

    private static boolean invokeStaticBoolean(ClassLoader classLoader, String className, String method) {
        try {
            return (boolean) Class.forName(className, true, classLoader).getMethod(method).invoke(null);
        } catch (ReflectiveOperationException | LinkageError cause) {
            throw new IllegalStateException("Glassless 0.13.0 or later is required", cause);
        }
    }
}
