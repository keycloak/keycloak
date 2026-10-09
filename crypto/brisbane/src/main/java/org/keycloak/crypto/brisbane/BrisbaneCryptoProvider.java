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

package org.keycloak.crypto.brisbane;

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
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;

import org.keycloak.common.crypto.CryptoConstants;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.elytron.WildFlyElytronProvider;

import org.jboss.logging.Logger;

public class BrisbaneCryptoProvider extends WildFlyElytronProvider {

    private static final Logger LOG = Logger.getLogger(BrisbaneCryptoProvider.class);
    private static final String PROVIDER_CLASS = "com.oracle.jipher.provider.JipherJCE";
    private static final String PROVIDER_NAME = "JipherJCE";
    private static final String FIPS_ENFORCEMENT = "jipher.fips.enforcement";

    private final Provider jipherProvider;

    public BrisbaneCryptoProvider() {
        this(false);
    }

    protected BrisbaneCryptoProvider(boolean strict) {
        this(resolveProvider(strict), strict);
    }

    BrisbaneCryptoProvider(Provider provider, boolean strict) {
        jipherProvider = provider;
        boolean inserted = Security.getProvider(PROVIDER_NAME) == null;
        if (inserted) {
            Security.insertProviderAt(jipherProvider, 1);
        }
        try {
            if (strict && Security.getProviders()[0] != jipherProvider) {
                throw new IllegalStateException("Brisbane must be the highest priority security provider in strict mode");
            }
            LOG.infof("BrisbaneCryptoProvider created: KC(%s, FIPS policy: %s)", jipherProvider,
                    System.getProperty(FIPS_ENFORCEMENT, "FIPS"));
        } catch (RuntimeException cause) {
            if (inserted) {
                Security.removeProvider(PROVIDER_NAME);
            }
            throw cause;
        }
    }

    private static Provider resolveProvider(boolean strict) {
        String requiredPolicy = strict ? "FIPS_STRICT" : "FIPS";
        String currentPolicy = System.getProperty(FIPS_ENFORCEMENT);
        if (currentPolicy != null && !requiredPolicy.equals(currentPolicy)) {
            throw new IllegalStateException("Brisbane requires " + FIPS_ENFORCEMENT + "=" + requiredPolicy);
        }
        if (Security.getProvider(PROVIDER_NAME) != null) {
            throw new IllegalStateException("Brisbane must be configured before Jipher is registered");
        }
        System.setProperty(FIPS_ENFORCEMENT, requiredPolicy);
        try {
            Object provider = Class.forName(PROVIDER_CLASS, true, Thread.currentThread().getContextClassLoader())
                    .getDeclaredConstructor().newInstance();
            if (provider instanceof Provider securityProvider) {
                return securityProvider;
            }
            throw new IllegalStateException(PROVIDER_CLASS + " is not a Java security provider");
        } catch (InvocationTargetException cause) {
            throw new IllegalStateException("Failed to initialize Brisbane", cause.getCause());
        } catch (ReflectiveOperationException | LinkageError cause) {
            throw new IllegalStateException("Brisbane 20.1 and Java 25 or later are required", cause);
        }
    }

    @Override
    public Provider getBouncyCastleProvider() {
        return jipherProvider;
    }

    @Override
    public <T> T getAlgorithmProvider(Class<T> clazz, String algorithm) {
        if (CryptoConstants.RSA1_5.equals(algorithm)) {
            throw new IllegalArgumentException("RSA1_5 key transport is not supported by Brisbane");
        }
        return super.getAlgorithmProvider(clazz, algorithm);
    }

    @Override
    public ECParameterSpec createECParams(String curveName) {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", jipherProvider);
            parameters.init(new ECGenParameterSpec(curveName));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (Exception cause) {
            throw new RuntimeException("Failed to generate EC parameter spec", cause);
        }
    }

    @Override
    public KeyPairGenerator getKeyPairGen(String algorithm) throws NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance("ECDSA".equals(algorithm) ? "EC" : algorithm, jipherProvider);
    }

    @Override
    public KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        return KeyFactory.getInstance("ECDSA".equals(algorithm) ? "EC" : algorithm, jipherProvider);
    }

    @Override
    public Cipher getAesCbcCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        Cipher cipher = super.getAesCbcCipher();
        if (cipher.getProvider() != jipherProvider) {
            throw new NoSuchAlgorithmException("Brisbane does not provide AES-CBC");
        }
        return cipher;
    }

    @Override
    public Cipher getAesGcmCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("AES/GCM/NoPadding", jipherProvider);
    }

    @Override
    public SecretKeyFactory getSecretKeyFact(String keyAlgorithm) throws NoSuchAlgorithmException {
        return SecretKeyFactory.getInstance(keyAlgorithm, jipherProvider);
    }

    @Override
    public Signature getSignature(String sigAlgName) throws NoSuchAlgorithmException {
        return Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(sigAlgName), jipherProvider);
    }
}
