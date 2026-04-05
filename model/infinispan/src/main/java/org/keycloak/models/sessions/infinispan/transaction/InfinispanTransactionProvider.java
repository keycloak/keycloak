/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.transaction;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.provider.Provider;

/**
 * A provider for Infinispan cache operations to be committed/rolled-back in a {@link KeycloakTransaction}.
 * <p>
 * It takes advantage of the non-blocking/async Infinispan API to requests the operations into the Infinispan caches
 * concurrently and waits, at the end, for all of them to complete.
 */
public interface InfinispanTransactionProvider extends Provider {

    /**
     * Registers a new {@link NonBlockingTransaction}.
     *
     * @param transaction The {@link NonBlockingTransaction} transaction instance.
     */
    void registerTransaction(NonBlockingTransaction transaction);

}
