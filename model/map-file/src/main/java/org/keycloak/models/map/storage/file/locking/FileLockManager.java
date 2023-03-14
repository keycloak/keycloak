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
import java.util.Map;
import java.util.function.Function;

import org.keycloak.Config;


/**
 * {@code FileLockManager} acts as a facade to the lock provider that has been configured for the file store. It instantiates
 * the proper provider according to the configuration and provides convenience methods to obtain {@link Lock} instances
 * from the provider.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FileLockManager {

    private static FileLockProvider provider;

    public static void init(final Config.Scope config, final Map<String, Function<String, Path>> rootAreaDirectories) {
        String lockProvider = config.get("lock-provider", "default");
        if (lockProvider.equals("no-op")) {
            provider = new NoOpLockProvider();
        } else {
            provider = new TempFileLockProvider(rootAreaDirectories, config.scope("lock-config"));
        }
    }

    /**
     * Create a lock object for the file in the specified {@link Path}. The returned lock is not active, meaning that it
     * is the responsibility of the caller to invoke {@link Lock#acquire()} and {@link Lock#release()} where appropriate.
     *
     * @param areaName the name of the area (model type) in which the lock is to be created.
     * @param realmId the realm in which the lock is to be created.
     * @param path the {@link Path} to the file for which a lock is wanted.
     * @return the created {@link Lock} instance.
     */
    public static Lock createLock(final String areaName, final String realmId, final Path path) {
        if (provider == null) {
            throw new IllegalStateException("File lock manager has not been initialized");
        }
        return provider.createLock(areaName, realmId, path);
    }

    /**
     * Attempts to forcefully release all currently active locks.
     */
    public static void forceReleaseAllLocks() {
        if (provider == null) {
            throw new IllegalStateException("File lock manager has not been initialized");
        }
        provider.releaseAllLocks();
    }


}
