package org.keycloak.common.crypto;

import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.common.util.BouncyIntegration;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CryptoIntegration {

    protected static final Logger logger = Logger.getLogger(CryptoIntegration.class);

    private static final Object lock = new Object();
    private static volatile CryptoProvider cryptoProvider;

    public static void init(ClassLoader classLoader) {
        if (cryptoProvider == null) {
            synchronized (lock) {
                if (cryptoProvider == null) {
                    cryptoProvider = detectProvider(classLoader);
                    logger.debugv("java security provider: {0}", BouncyIntegration.PROVIDER);

                }
            }
        }

        if (logger.isTraceEnabled()) {
            logger.tracef(dumpJavaSecurityProviders());
            logger.tracef(dumpSecurityProperties());
        }
    }

    public static boolean isInitialised() {
        return cryptoProvider != null;
    }

    public static CryptoProvider getProvider() {
        if (cryptoProvider == null) {
            throw new IllegalStateException("Illegal state. Please init first before obtaining provider");
        }
        return cryptoProvider;
    }


    // Try to auto-detect provider
    private static CryptoProvider detectProvider(ClassLoader classLoader) {
        List<CryptoProvider> foundProviders = StreamSupport.stream(ServiceLoader.load(CryptoProvider.class, classLoader).spliterator(), false)
                .sorted(Comparator.comparingInt(CryptoProvider::order).reversed())
                .collect(Collectors.toList());

        if (foundProviders.isEmpty()) {
            throw new IllegalStateException("Not able to load any cryptoProvider with the classLoader: " + classLoader);
        } else {
            logger.debugf("Detected crypto provider: %s", foundProviders.get(0).getClass().getName());
            if (foundProviders.size() > 1) {
                StringBuilder builder = new StringBuilder("Ignored crypto providers: ");
                for (int i = 1 ; i < foundProviders.size() ; i++) {
                    builder.append(foundProviders.get(i).getClass().getName() + ", ");
                }
                logger.debugf(builder.toString());
            }
            return foundProviders.get(0);
        }
    }

    public static String dumpJavaSecurityProviders() {
        StringBuilder builder = new StringBuilder("Java security providers: [ \n");
        for (Provider p : Security.getProviders()) {
            builder.append(" " + p.toString() + " - " + p.getClass() + ", \n");
        }
        return builder.append("]").toString();
    }

    public static String dumpSecurityProperties() {
        StringBuilder builder = new StringBuilder("Security properties: [ \n")
                .append(" Java security properties file: " + System.getProperty("java.security.properties") + "\n")
                .append(" Default keystore type: " + KeyStore.getDefaultType() + "\n")
                .append(" KeyManagerFactory.getDefaultAlgorithm(): " + KeyManagerFactory.getDefaultAlgorithm() + "\n")
                .append(" TrustManagerFactory.getDefaultAlgorithm(): " + TrustManagerFactory.getDefaultAlgorithm() + "\n")
                .append(" keystore.type.compat: " + Security.getProperty("keystore.type.compat") + "\n");
        Stream.of("javax.net.ssl.trustStoreType", "javax.net.ssl.trustStore", "javax.net.ssl.trustStoreProvider",
                        "javax.net.ssl.keyStoreType", "javax.net.ssl.keyStore", "javax.net.ssl.keyStoreProvider")
                .forEach(propertyName -> builder.append(" " + propertyName + ": " + System.getProperty(propertyName) + "\n"));
        return builder.append("]").toString();
    }

    public static void setProvider(CryptoProvider provider) {
        logger.debugf("Using the crypto provider: %s", provider != null ? provider.getClass().getName() : "null");
        cryptoProvider = provider;
    }
}
