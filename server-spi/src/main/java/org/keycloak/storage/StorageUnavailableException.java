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

package org.keycloak.storage;

/**
 * Exception thrown by user storage providers to indicate that the external storage
 * system is temporarily unavailable due to connectivity issues, server downtime,
 * or other infrastructure problems.
 * 
 * <p>This exception allows storage providers to signal graceful degradation scenarios
 * where the UserStorageManager should skip the unavailable provider and continue
 * with other available providers or local storage.</p>
 *
 */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException() {
        super();
    }

    public StorageUnavailableException(String message) {
        super(message);
    }

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageUnavailableException(Throwable cause) {
        super(cause != null ? cause.getMessage() : null, cause);
    }
}
