package org.keycloak.vault;

import java.util.Optional;

/**
 * Default {@link VaultCharSecret} implementation based on {@link String}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class DefaultVaultStringSecret implements VaultStringSecret {

    private static final VaultStringSecret EMPTY_VAULT_SECRET = new VaultStringSecret() {
        @Override
        public Optional<String> get() {
            return Optional.empty();
        }

        @Override
        public void close() {
        }
    };

    public static VaultStringSecret forString(Optional<String> secret) {
        if (secret == null || ! secret.isPresent()) {
            return EMPTY_VAULT_SECRET;
        }
        return new DefaultVaultStringSecret(secret.get());
    }

    private String secret;

    private DefaultVaultStringSecret(final String secret) {
        this.secret = secret;
    }

    @Override
    public Optional<String> get() {
        return Optional.of(this.secret);
    }

    @Override
    public void close() {
        this.secret = null;
    }
}
