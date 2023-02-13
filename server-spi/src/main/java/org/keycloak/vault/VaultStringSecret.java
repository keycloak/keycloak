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

import java.util.Optional;

/**
 * A {@link String} based representation of the secret obtained from the vault that supports automated cleanup of memory.
 * In this case, due to the immutable nature of strings, the cleanup should consist in releasing any references to the
 * secret string so it can be disposed by the GC as soon as possible.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public interface VaultStringSecret extends AutoCloseable {

    /**
     * Returns the secret represented as a {@link String}.
     * @return If the secret was successfully resolved by vault, returns an {@link Optional} containing the value returned
     *         by the vault as a {@link String} (a valid value can be {@code null}), or an empty {@link Optional}
     */
    Optional<String> get();

    /**
     *  Destroys the secret in memory by e.g. overwriting it with random garbage or release references in case of immutable
     *  secrets.
     */
    @Override
    void close();
}
