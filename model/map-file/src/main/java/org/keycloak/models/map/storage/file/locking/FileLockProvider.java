/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.file.locking;

import java.nio.file.Path;

/**
 * Interface that defines a file lock provider.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public interface FileLockProvider {

    /**
     * Create a {@link Lock} instance with the specified parameters. This only creates the lock object, the actual lock
     * acquisition is only performed once {@link Lock#acquire()} is called.
     *
     * @param areaName the name of the area (model type) in which the lock must be created.
     * @param realmId the realm in which the lock must be created.
     * @param path the {@link Path} to the file for which a lock is wanted.
     * @return the created {@link Lock} instance.
     */
    Lock createLock(final String areaName, final String realmId, final Path path);

    /**
     * Forcefully releases all locks. Implementations must attempt to remove all locks that might be active (i.e. those
     * resulting from calling {@link Lock#acquire()}.
     */
    void releaseAllLocks();
}
