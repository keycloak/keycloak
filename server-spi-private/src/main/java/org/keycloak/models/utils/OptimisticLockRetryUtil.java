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

package org.keycloak.models.utils;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelIllegalStateException;

import java.util.function.Supplier;

public class OptimisticLockRetryUtil {

    private static final Logger logger = Logger.getLogger(OptimisticLockRetryUtil.class);

    private static final int DEFAULT_MAX_RETRIES = 5;

    /**
     * Retry an operation with default maximum retry count (5).
     *
     * @param session   the Keycloak session for transaction management
     * @param operation the operation to retry
     * @param <T>       the return type
     * @return the result of the operation
     * @throws ModelIllegalStateException if not an optimistic lock exception or max retries exceeded
     */
    public static <T> T retry(KeycloakSession session, Supplier<T> operation) {
        return retry(session, operation, DEFAULT_MAX_RETRIES);
    }

    /**
     * Retry an operation with specified maximum retry count.
     *
     * @param session    the Keycloak session for transaction management
     * @param operation  the operation to retry
     * @param maxRetries maximum number of retry attempts
     * @param <T>        the return type
     * @return the result of the operation
     * @throws ModelIllegalStateException if not an optimistic lock exception or max retries exceeded
     */
    public static <T> T retry(KeycloakSession session, Supplier<T> operation, int maxRetries) {
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                return operation.get();
            } catch (ModelIllegalStateException e) {
                if (isOptimisticLockException(e) && attempt < maxRetries) {
                    logger.debugf("Optimistic lock exception (attempt %d/%d), retrying...", attempt, maxRetries);
                    session.getTransactionManager().setRollbackOnly();
                } else {
                    // Not an optimistic lock exception or max retries exceeded
                    throw e;
                }
            }
        }

        // This should never be reached as we throw in the catch block when attempts are exhausted
        throw new ModelIllegalStateException("Failed after " + maxRetries + " attempts due to optimistic locking conflicts");
    }

    /**
     * Check if the exception is caused by an optimistic lock conflict.
     *
     * @param e the exception to check
     * @return true if this is an optimistic lock exception
     */
    private static boolean isOptimisticLockException(ModelIllegalStateException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            String className = cause.getClass().getName();
            // Check for both JPA and Hibernate OptimisticLock exceptions
            if (className.contains("OptimisticLock")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
