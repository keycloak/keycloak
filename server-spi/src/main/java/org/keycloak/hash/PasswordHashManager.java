package org.keycloak.hash;

import org.jboss.logging.Logger;
import org.keycloak.models.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordHashManager {

    private static final Logger log = Logger.getLogger(PasswordHashManager.class);

    public static UserCredentialValueModel encode(KeycloakSession session, RealmModel realm, String rawPassword) {
        return encode(session, realm.getPasswordPolicy(), rawPassword);
    }

    public static UserCredentialValueModel encode(KeycloakSession session, PasswordPolicy passwordPolicy, String rawPassword) {
        String algorithm = passwordPolicy.getHashAlgorithm();
        int iterations = passwordPolicy.getHashIterations();
        if (iterations < 1) {
            iterations = 1;
        }
        PasswordHashProvider provider = session.getProvider(PasswordHashProvider.class, passwordPolicy.getHashAlgorithm());
        if (provider == null) {
            log.warnv("Could not find hash provider {0} from password policy, using default provider {1}", algorithm, Constants.DEFAULT_HASH_ALGORITHM);
            provider = session.getProvider(PasswordHashProvider.class, Constants.DEFAULT_HASH_ALGORITHM);
        }
        return provider.encode(rawPassword, iterations);
    }

    public static boolean verify(KeycloakSession session, RealmModel realm, String password, UserCredentialValueModel credential) {
        return verify(session, realm.getPasswordPolicy(), password, credential);
    }

    public static boolean verify(KeycloakSession session, PasswordPolicy passwordPolicy, String password, UserCredentialValueModel credential) {
        String algorithm = credential.getAlgorithm() != null ? credential.getAlgorithm() : passwordPolicy.getHashAlgorithm();
        PasswordHashProvider provider = session.getProvider(PasswordHashProvider.class, algorithm);
        if (provider == null) {
            log.warnv("Could not find hash provider {0} for password", algorithm);
            return false;
        }
        return provider.verify(password, credential);
    }

}
