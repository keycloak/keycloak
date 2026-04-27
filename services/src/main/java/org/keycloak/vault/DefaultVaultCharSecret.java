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

import java.nio.CharBuffer;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Default {@link VaultCharSecret} implementation based on {@link CharBuffer}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class DefaultVaultCharSecret implements VaultCharSecret {

    private static final VaultCharSecret EMPTY_VAULT_SECRET = new VaultCharSecret() {
        @Override
        public Optional<CharBuffer> get() {
            return Optional.empty();
        }

        @Override
        public Optional<char[]> getAsArray() {
            return Optional.empty();
        }

        @Override
        public void close() {
        }
    };

    public static VaultCharSecret forBuffer(Optional<CharBuffer> buffer) {
        if (buffer == null || ! buffer.isPresent()) {
            return EMPTY_VAULT_SECRET;
        }
        return new DefaultVaultCharSecret(buffer.get());
    }

    private final CharBuffer buffer;

    private char[] secretArray;

    private DefaultVaultCharSecret(final CharBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public Optional<CharBuffer> get() {
        return Optional.of(this.buffer);
    }

    @Override
    public Optional<char[]> getAsArray() {
        if (this.secretArray == null) {
            // initialize internal array on demand.
            if (this.buffer.hasArray()) {
                this.secretArray = buffer.array();
            } else {
                secretArray = new char[buffer.capacity()];
                buffer.get(secretArray);
            }
        }
        return Optional.of(this.secretArray);
    }

    @Override
    public void close() {
        if (this.buffer.hasArray()) {
            char[] internalArray = this.buffer.array();
            for (int i = 0; i < internalArray.length; i++) {
                internalArray[i] = (char) ThreadLocalRandom.current().nextInt();
            }
        } else if (this.secretArray != null) {
            for (int i = 0; i < this.secretArray.length; i++) {
                this.secretArray[i] = (char) ThreadLocalRandom.current().nextInt();
            }
        }
        this.buffer.clear();
    }
}