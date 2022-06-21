package org.keycloak.crypto.integration;

import java.util.ServiceLoader;

import org.jboss.logging.Logger;
import org.keycloak.common.util.BouncyIntegration;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CryptoIntegration {

    protected static final Logger logger = Logger.getLogger(CryptoIntegration.class);

    private static volatile CryptoProvider securityProvider;

    public static CryptoProvider getProvider() {
        if (securityProvider == null) {
            logger.debugf("Using BouncyCastle provider: %s", BouncyIntegration.PROVIDER);
            securityProvider = detectProvider();
            logger.infof("Detected security provider: %s", securityProvider);
        }
        return securityProvider;
    }


    // This can be possibly set by the configuration (SPI) to override the "detected" instance
    public static void setProvider(CryptoProvider provider) {
        securityProvider = provider;
    }


    // Try to auto-detect provider
    private static CryptoProvider detectProvider() {
        // TODO This may not work on Wildfly (assuming FIPS module will be different Wildfly module than keycloak-core). May need to be improved (EG. with usage of org.keycloak.platform.Platform)
        for (CryptoProvider cryptoProvider : ServiceLoader.load(CryptoProvider.class, CryptoIntegration.class.getClassLoader())) {
            return cryptoProvider;
        }
        // Fallback. This should not be needed once DefaultCryptoProvider is moved into separate module like "crypto/default" and provided via ServiceLoader
        return new DefaultCryptoProvider();
    }

}
