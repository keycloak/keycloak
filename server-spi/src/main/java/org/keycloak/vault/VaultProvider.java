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

import org.keycloak.provider.Provider;

/**
 * Provider interface for a vault. The only purpose of a vault is retrieval of secrets.
 */
public interface VaultProvider extends Provider {

    /**
     * Retrieves a secret from vault. The implementation should respect
     * at least the realm ID to separate the secrets within the vault.
     * If the secret is retrieved successfully, it is returned;
     * otherwise this method results into an empty {@link VaultRawSecret#get()}.
     *
     * This method is intended to be used within a try-with-resources block so that
     * the secret is destroyed immediately after use.
     *
     * Note that it is responsibility of the implementor to provide a way
     * to destroy the secret in the returned {@link VaultRawSecret#close()} method.
     *
     * @param vaultSecretId Identifier of the secret. It corresponds to the value
     *        entered by user in the respective configuration, which in turn
     *        is obtained from the vault when storing the secret.
     *
     * @return Always a non-{@code null} value with the raw secret.
     *         Within the returned value, the secret or {@code null} is stored in the
     *         {@link VaultRawSecret#get()} return value if the secret was successfully
     *         resolved, or an empty {@link java.util.Optional} if the secret has not been found in the vault.
     */
    VaultRawSecret obtainSecret(String vaultSecretId);

}
