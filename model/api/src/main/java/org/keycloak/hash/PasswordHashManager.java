package org.keycloak.hash;

import org.keycloak.models.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordHashManager {

    public static UserCredentialValueModel encode(KeycloakSession session, RealmModel realm, String rawPassword) {
        PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
        String algorithm = passwordPolicy.getHashAlgorithm();
        int iterations = passwordPolicy.getHashIterations();
        if (iterations < 1) {
            iterations = 1;
        }
        PasswordHashProvider provider = session.getProvider(PasswordHashProvider.class, algorithm);
        if (provider == null) {
            throw new RuntimeException("Password hash provider for algorithm " + algorithm + " not found");
        }
        return provider.encode(rawPassword, iterations);
    }

    public static boolean verify(KeycloakSession session, RealmModel realm, String password, UserCredentialValueModel credential) {
        String algorithm = credential.getAlgorithm() != null ? credential.getAlgorithm() : realm.getPasswordPolicy().getHashAlgorithm();
        PasswordHashProvider provider = session.getProvider(PasswordHashProvider.class, algorithm);
        return provider.verify(password, credential);
    }

}
