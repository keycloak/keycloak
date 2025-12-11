/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.vault;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

/**
 * Abstract class that is meant to be extended by implementations of {@link VaultProvider} that want to have support for
 * key resolvers.
 * <p/>
 * This class implements the {@link #obtainSecret(String)} method by iterating through the configured resolvers in order and,
 * using the final key name provided by each resolver, calls the {@link #obtainSecretInternal(String)} method that must be
 * implemented by sub-classes. If {@link #obtainSecretInternal(String)} returns a non-empty secret, it is immediately returned;
 * otherwise the implementation tries again using the next configured resolver until a non-empty secret is obtained or all
 * resolvers have been tried, in which case an empty {@link VaultRawSecret} is returned.
 * <p/>
 * Concrete implementations must, in addition to implementing the {@link #obtainSecretInternal(String)} method, ensure that
 * each constructor calls the {@link AbstractVaultProvider#AbstractVaultProvider(String, List)} constructor from this class
 * so that the realm and list of key resolvers are properly initialized.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public abstract class AbstractVaultProvider implements VaultProvider {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    protected final String realm;
    protected final List<VaultKeyResolver> resolvers;


    /**
     * Creates an instance of {@code AbstractVaultProvider} with the specified realm and list of key resolvers.
     *
     * @param realm the name of the keycloak realm.
     * @param configuredResolvers a {@link List} containing the configured key resolvers.
     */
    public AbstractVaultProvider(final String realm, final List<VaultKeyResolver> configuredResolvers) {
        this.realm = realm;
        this.resolvers = configuredResolvers;
    }

    @Override
    public VaultRawSecret obtainSecret(String vaultSecretId) {
        for (VaultKeyResolver resolver : this.resolvers) {
            String resolvedKey = resolver.apply(this.realm, vaultSecretId);
            if (!validate(resolver, vaultSecretId, resolvedKey)) {
                logger.warnf("Validation failed for secret %s with resolved key %s", vaultSecretId, resolvedKey);
                return DefaultVaultRawSecret.forBuffer(Optional.empty());
            }
        }

        for (VaultKeyResolver resolver : this.resolvers) {
            String resolvedKey = resolver.apply(this.realm, vaultSecretId);
            VaultRawSecret secret = this.obtainSecretInternal(resolvedKey);
            if (secret != null && secret.get().isPresent()) {
                return secret;
            }
            checkForLegacyKey(resolver, vaultSecretId, resolvedKey);
        }
        return DefaultVaultRawSecret.forBuffer(Optional.empty());
    }

    private void checkForLegacyKey(VaultKeyResolver resolver, String vaultSecretId, String resolvedKey) {
        if (resolver == AbstractVaultProviderFactory.AvailableResolvers.KEY_ONLY.getVaultKeyResolver() && vaultSecretId.contains("_")) {
            String legacyKey = vaultSecretId.replaceAll("__", "_");
            VaultRawSecret legacySecret = this.obtainSecretInternal(legacyKey);
            if (legacySecret != null && legacySecret.get().isPresent()) {
                logger.warnf("Secret was found using legacy key '%s'. Please rename the key to '%s' and repeat the action.", legacyKey, resolvedKey);
            }
        }
    }

    /**
     * Validates the resolved key to ensure it meets the necessary criteria.
     *
     * @param resolver the {@link VaultKeyResolver} used to resolve the key.
     * @param key the original key provided.
     * @param resolvedKey the key after being resolved by the resolver.
     * @return a boolean indicating whether the validation passed.
     */
    protected boolean validate(VaultKeyResolver resolver, String key, String resolvedKey) {
        if (key.contains(File.separator)) {
            logger.warnf("Key %s contains invalid file separator character", key);
            return false;
        }
        return true;
    }

    /**
     * Subclasses of {@code AbstractVaultProvider} must implement this method. It is meant to be implemented in the same
     * way as the {@link #obtainSecret(String)} method from the {@link VaultProvider} interface, but the specified vault
     * key must be used as is - i.e. implementations should refrain from processing the key again as the format was already
     * defined by one of the configured key resolvers.
     *
     * @param vaultKey a {@link String} representing the name of the entry that is being fetched from the vault.
     * @return a {@link VaultRawSecret} representing the obtained secret. It can be a empty secret if no secret could be
     * obtained using the specified vault key.
     */
    protected abstract VaultRawSecret obtainSecretInternal(final String vaultKey);

}
