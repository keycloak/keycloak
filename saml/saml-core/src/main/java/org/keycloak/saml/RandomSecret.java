package org.keycloak.saml;

import java.security.SecureRandom;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RandomSecret {
    /**
     * <p>
     * Creates a random {@code byte[]} secret of the specified size.
     * </p>
     *
     * @param size the size of the secret to be created, in bytes.
     *
     * @return a {@code byte[]} containing the generated secret.
     */
    public static byte[] createRandomSecret(final int size) {
        SecureRandom random = new SecureRandom();
        byte[] secret = new byte[size];
        random.nextBytes(secret);
        return secret;
    }
}
