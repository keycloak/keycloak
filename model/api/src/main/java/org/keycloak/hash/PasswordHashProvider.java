package org.keycloak.hash;

import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:me@tsudot.com">Kunal Kerkar</a>
 */
public interface PasswordHashProvider extends Provider {

    String encode(String rawPassword, byte[] salt);

    String encode(String rawPassword, byte[] salt, int iterations);

    boolean verify(String rawPassword, String encodedPassword, byte[] salt);

}
