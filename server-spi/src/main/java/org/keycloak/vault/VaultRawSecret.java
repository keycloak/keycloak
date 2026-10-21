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

/**
 *  Raw representation of the secret obtained from vault that supports automated cleanup of memory.
 *
 *  @author hmlnarik
 */
public interface VaultRawSecret extends AutoCloseable {

    /**
     * Returns the raw secret bytes.
     * @return If the secret was successfully resolved by vault, returns
     *         an {@link Optional} containing the value returned by the vault
     *         (a valid value can be {@code null}), or an empty {@link Optional}
     */
    Optional<ByteBuffer> get();

    /**
     * Returns the raw secret bytes in {@code byte[]} form.
     * @return If the secret was successfully resolved by vault, returns
     *         an {@link Optional} containing the value returned by the vault
     *         (a valid value can be {@code null}), or an empty {@link Optional}
     */
    Optional<byte[]> getAsArray();

    /**
     *  Destroys the secret in memory by e.g. overwriting it with random garbage.
     */
    @Override
    void close();

}
