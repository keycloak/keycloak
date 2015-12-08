package org.keycloak.hash;

import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:me@tsudot.com">Kunal Kerkar</a>
 */
public interface PasswordHashProvider extends Provider {

    UserCredentialValueModel encode(String rawPassword, int iterations);

    boolean verify(String rawPassword, UserCredentialValueModel credential);

}
