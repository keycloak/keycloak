/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.vault;

import static org.keycloak.vault.DefaultVaultRawSecret.forBuffer;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.vault.AbstractVaultProvider;
import org.keycloak.vault.VaultKeyResolver;
import org.keycloak.vault.VaultRawSecret;

import io.quarkus.vault.VaultKVSecretEngine;

public class QuarkusVaultProvider extends AbstractVaultProvider {

    private VaultKVSecretEngine secretEngine;
    private String[] kvPaths;

    public QuarkusVaultProvider(VaultKVSecretEngine secretEngine, String[] kvPaths, String realm, List<VaultKeyResolver> keyResolvers) {
        super(realm, keyResolvers);
        this.secretEngine = secretEngine;
        this.kvPaths = kvPaths;
    }

    @Override
    protected VaultRawSecret obtainSecretInternal(String key) {
        if (kvPaths == null) {
            return forBuffer(Optional.empty());
        }

        for (String path : kvPaths) {
            Map<String, String> secrets = secretEngine.readSecret(path);
            String secret = secrets.get(key);

            if (secret != null) {
                return forBuffer(Optional.of(StandardCharsets.UTF_8.encode(CharBuffer.wrap(secret))));
            }
        }

        return forBuffer(Optional.empty());
    }

    @Override
    public void close() {

    }
}
