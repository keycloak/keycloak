package org.keycloak.crypto.integration;

import org.jboss.logging.Logger;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.reflections.Reflections;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CryptoIntegration {

    protected static final Logger logger = Logger.getLogger(CryptoIntegration.class);

    private static volatile CryptoProvider securityProvider;

    public static CryptoProvider getProvider() {
        if (securityProvider == null) {
            securityProvider = detectProvider();
            logger.infof("Detected security provider: %s", securityProvider);
        }
        return securityProvider;
    }


    // This can be possibly set by the configuration (SPI) to override the "detected" instance
    public static void setProvider(CryptoProvider provider) {
        securityProvider = provider;
    }


    // Try to auto-detect provider. Currently autodetected based on whether we have BC or BCFIPS on the classpath. Should be probably improved and rather
    // detected based on the system security settings or with ServiceLoader etc. Ideally, the BouncyCastle implementation should be chosen based on the SecurityProvider rather than vice-versa.
    private static CryptoProvider detectProvider() {
        if (BouncyIntegration.PROVIDER.equals("BCFIPS")) {
            try {
                // TODO This may not work on Wildfly (assuming FIPS module will be different Wildfly module than keycloak-core). May need to be improved (EG. with usage of org.keycloak.platform.Platform)
                Class<CryptoProvider> clazz = Reflections.classForName("org.keycloak.crypto.fips.FIPS1402Provider", CryptoIntegration.class.getClassLoader());
                return clazz.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Exception when trying to create FIPS provider. Thread classloader: " + Thread.currentThread().getContextClassLoader(), e);
            }
        } else {
            return new DefaultCryptoProvider();
        }
    }

}
