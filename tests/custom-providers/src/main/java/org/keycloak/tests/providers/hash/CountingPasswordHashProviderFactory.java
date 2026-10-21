package org.keycloak.tests.providers.hash;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.credential.PasswordCredentialModel;

/**
 * Performs real PBKDF2 hashing and records completed encode and verify operations.
 * Intended for sequential tests using this algorithm in one realm at a time.
 * Tests can select {@link #ID} in the realm's password policy and use
 * {@link #getAndResetHashOperations(KeycloakSession)} before and after a login attempt to assert
 * hashing work and iteration counts, catching user enumeration regressions without timing assertions.
 */
public class CountingPasswordHashProviderFactory extends Pbkdf2Sha256PasswordHashProviderFactory {

    public static final String ID = "counting-pbkdf2-sha256";

    private final List<String> operations = new CopyOnWriteArrayList<>();

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Pbkdf2PasswordHashProvider(ID, PBKDF2_ALGORITHM, DEFAULT_ITERATIONS, getMaxPaddingLength(), 256) {
            @Override
            public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
                PasswordCredentialModel credential = super.encodedCredential(rawPassword, iterations);
                record("encode", credential);
                return credential;
            }

            @Override
            public boolean verify(String rawPassword, PasswordCredentialModel credential) {
                boolean valid = super.verify(rawPassword, credential);
                record("verify", credential);
                return valid;
            }
        };
    }

    private void record(String operation, PasswordCredentialModel credential) {
        operations.add(operation + ":" + credential.getPasswordCredentialData().getHashIterations());
    }

    /**
     * Returns and clears the recorded operations as {@code operation:iterations}.
     * {@code encode} creates a new salted hash, including dummy hashes; {@code verify} checks
     * a submitted password against a stored credential. The iteration count comes from the
     * credential, so {@code encode:1000} is an example, not a fixed value.
     */
    public static List<String> getAndResetHashOperations(KeycloakSession session) {
        CountingPasswordHashProviderFactory factory = (CountingPasswordHashProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(PasswordHashProvider.class, ID);
        List<String> recorded = List.copyOf(factory.operations);
        factory.operations.clear();
        return recorded;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int order() {
        return -1;
    }
}
