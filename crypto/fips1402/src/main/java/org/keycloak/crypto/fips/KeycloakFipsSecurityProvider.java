package org.keycloak.crypto.fips;

import java.security.Provider;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.jboss.logging.Logger;

/**
 * Security provider to workaround usage of potentially unsecured algorithms by 3rd party dependencies.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakFipsSecurityProvider extends Provider {

    protected static final Logger logger = Logger.getLogger(KeycloakFipsSecurityProvider.class);

    private final BouncyCastleFipsProvider bcFipsProvider;

    public KeycloakFipsSecurityProvider(BouncyCastleFipsProvider bcFipsProvider) {
        super("KC", 1, "Keycloak pseudo provider");
        this.bcFipsProvider = bcFipsProvider;
    }


    @Override
    public synchronized final Service getService(String type, String algorithm) {
        // Using 'SecureRandom.getInstance("SHA1PRNG")' will delegate to BCFIPS DEFAULT provider instead of returning SecureRandom based on potentially unsecure SHA1PRNG
        if ("SHA1PRNG".equals(algorithm) && "SecureRandom".equals(type)) {
            logger.debug("Returning DEFAULT algorithm of BCFIPS provider instead of SHA1PRNG");
            return this.bcFipsProvider.getService("SecureRandom", "DEFAULT");
        } else {
            return null;
        }
    }
}
