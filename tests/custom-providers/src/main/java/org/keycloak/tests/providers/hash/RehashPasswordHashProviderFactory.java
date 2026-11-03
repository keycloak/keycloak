package org.keycloak.tests.providers.hash;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.PasswordCredentialModel;

// Runs a realm-scoped action once during hashing or before commit for deterministic concurrent credential updates.
public class RehashPasswordHashProviderFactory extends Pbkdf2Sha512PasswordHashProviderFactory {

    public static final String ID = "test-rehash-pbkdf2-sha512";

    public enum Phase {
        DURING_HASH,
        BEFORE_COMMIT
    }

    private final Map<String, Consumer<KeycloakSession>> actions = new ConcurrentHashMap<>();

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Pbkdf2PasswordHashProvider(ID, PBKDF2_ALGORITHM, DEFAULT_ITERATIONS, getMaxPaddingLength()) {
            @Override
            public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
                PasswordCredentialModel credential = super.encodedCredential(rawPassword, iterations);
                RealmModel realm = session.getContext().getRealm();
                Consumer<KeycloakSession> action = realm != null ? actions.remove(realm.getId()) : null;
                if (action != null) {
                    action.accept(session);
                }
                return credential;
            }
        };
    }

    public static void onNextHash(KeycloakSession session, Phase phase, Consumer<KeycloakSession> action) {
        factory(session).actions.put(session.getContext().getRealm().getId(), rehashSession -> {
            if (phase == Phase.DURING_HASH) {
                action.accept(rehashSession);
            } else {
                // Prepare runs after the rehash reads the credential, before its JPA transaction commits.
                rehashSession.getTransactionManager().enlistPrepare(new AbstractKeycloakTransaction() {
                    @Override
                    protected void commitImpl() {
                        action.accept(rehashSession);
                    }

                    @Override
                    protected void rollbackImpl() {
                    }
                });
            }
        });
    }

    public static void clearAction(KeycloakSession session) {
        factory(session).actions.remove(session.getContext().getRealm().getId());
    }

    private static RehashPasswordHashProviderFactory factory(KeycloakSession session) {
        return (RehashPasswordHashProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(PasswordHashProvider.class, ID);
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
