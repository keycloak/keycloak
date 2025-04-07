package org.keycloak.compatibility;

import java.util.Optional;

/**
 * Internal class to signal that the provider is compatible with the previous metadata.
 */
record ProviderCompatibleResult(String providerId) implements CompatibilityResult {

    @Override
    public int exitCode() {
        return ExitCode.ROLLING.value();
    }

    @Override
    public Optional<String> endMessage() {
        return Optional.of("[%s] Provider is compatible.".formatted(providerId));
    }
}
