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

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Default raw secret implementation for {@code byte[]}.
 * @author hmlnarik
 */
public class DefaultVaultRawSecret implements VaultRawSecret {

    private static final VaultRawSecret EMPTY_VAULT_SECRET = new VaultRawSecret() {
        @Override
        public Optional<ByteBuffer> getRawSecret() {
            return Optional.empty();
        }

        @Override
        public void close() {
        }
    };

    private final ByteBuffer rawSecret;

    public static VaultRawSecret forBuffer(Optional<ByteBuffer> buffer) {
        if (buffer == null || ! buffer.isPresent()) {
            return EMPTY_VAULT_SECRET;
        }
        return new DefaultVaultRawSecret(buffer.get());
    }

    private DefaultVaultRawSecret(ByteBuffer rawSecret) {
        this.rawSecret = rawSecret;
    }

    @Override
    public Optional<ByteBuffer> getRawSecret() {
        return Optional.of(this.rawSecret);
    }

    @Override
    public void close() {
        if (rawSecret.hasArray()) {
            ThreadLocalRandom.current().nextBytes(rawSecret.array());
        }
    }
}
