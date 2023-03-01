/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.locking;

import java.time.Instant;

/**
 * This exception is thrown when acquiring a lock times out.
 */
public final class LockAcquiringTimeoutException extends RuntimeException {

    /**
     *
     * @param lockName Identifier of a lock whose acquiring was unsuccessful.
     * @param keycloakInstanceIdentifier Identifier of a Keycloak instance that is currently holding the lock.
     * @param timeWhenAcquired Time instant when the lock held by {@code keycloakInstanceIdentifier} was acquired.
     */
    public LockAcquiringTimeoutException(String lockName, String keycloakInstanceIdentifier, Instant timeWhenAcquired) {
        super(String.format("Lock [%s] already acquired by keycloak instance [%s] at the time [%s]", lockName, keycloakInstanceIdentifier, timeWhenAcquired.toString()));
    }

    /**
     *
     * @param lockName Identifier of a lock whose acquiring was unsuccessful.
     * @param keycloakInstanceIdentifier Identifier of a Keycloak instance that is currently holding the lock.
     * @param timeWhenAcquired Time instant when the lock held by {@code keycloakInstanceIdentifier} was acquired.
     * @param cause The cause.
     */
    public LockAcquiringTimeoutException(String lockName, String keycloakInstanceIdentifier, Instant timeWhenAcquired, Throwable cause) {
        super(String.format("Lock [%s] already acquired by keycloak instance [%s] at the time [%s]", lockName, keycloakInstanceIdentifier, timeWhenAcquired.toString()), cause);
    }
}
