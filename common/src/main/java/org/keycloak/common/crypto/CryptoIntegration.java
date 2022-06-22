package org.keycloak.common.crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CryptoIntegration {

    protected static final Logger logger = Logger.getLogger(CryptoIntegration.class);

    private static volatile CryptoProvider securityProvider;

    private static volatile ClassLoader classLoader;

    public static void setClassLoader(ClassLoader classLoader) {
        CryptoIntegration.classLoader = classLoader;
    }

    public static CryptoProvider getProvider() {
        if (securityProvider == null) {
            securityProvider = detectProvider();
            logger.infof("Detected security provider: %s", securityProvider.getClass().getName());
        }
        return securityProvider;
    }


    // This can be possibly set by the configuration (SPI) to override the "detected" instance
    public static void setProvider(CryptoProvider provider) {
        securityProvider = provider;
    }


    // Try to auto-detect provider
    private static CryptoProvider detectProvider() {
        ClassLoader classLoader = CryptoIntegration.classLoader != null ? CryptoIntegration.classLoader : CryptoIntegration.class.getClassLoader();
        List<CryptoProvider> foundProviders = StreamSupport.stream(ServiceLoader.load(CryptoProvider.class, classLoader).spliterator(), false)
                .collect(Collectors.toList());

        if (foundProviders.isEmpty()) {
            throw new IllegalStateException("Not able to load any cryptoProvider with the classLoader: " + classLoader);
        } else if (foundProviders.size() > 1) {
            throw new IllegalStateException("Multiple crypto providers loaded with the classLoader: " + classLoader +
                    ". Make sure only one cryptoProvider available on the classpath. Available providers: " +foundProviders);
        } else {
            return foundProviders.get(0);
        }
    }

}
