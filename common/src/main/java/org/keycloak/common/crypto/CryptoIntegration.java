package org.keycloak.common.crypto;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.jboss.logging.Logger;
import org.keycloak.common.util.BouncyIntegration;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CryptoIntegration {

    protected static final Logger logger = Logger.getLogger(CryptoIntegration.class);

    private static volatile AtomicReference<CryptoProvider> securityProvider = new AtomicReference<>();

    public static void init(ClassLoader classLoader) {
        securityProvider.set(detectProvider(classLoader));
        logger.debugv("BouncyCastle provider: {0}", BouncyIntegration.PROVIDER);
    }

    public static CryptoProvider getProvider() {
        CryptoProvider cryptoProvider = securityProvider.get();
        if (cryptoProvider == null) {
            securityProvider.compareAndSet(null, detectProvider(CryptoIntegration.class.getClassLoader()));
            cryptoProvider = securityProvider.get();
        }
        return cryptoProvider;
    }


    // Try to auto-detect provider
    private static CryptoProvider detectProvider(ClassLoader classLoader) {
        List<CryptoProvider> foundProviders = StreamSupport.stream(ServiceLoader.load(CryptoProvider.class, classLoader).spliterator(), false)
                .collect(Collectors.toList());

        if (foundProviders.isEmpty()) {
            throw new IllegalStateException("Not able to load any cryptoProvider with the classLoader: " + classLoader);
        } else if (foundProviders.size() > 1) {
            throw new IllegalStateException("Multiple crypto providers loaded with the classLoader: " + classLoader +
                    ". Make sure only one cryptoProvider available on the classpath. Available providers: " +foundProviders);
        } else {
            logger.infof("Detected security provider: %s", securityProvider.getClass().getName());
            return foundProviders.get(0);
        }
    }

}
