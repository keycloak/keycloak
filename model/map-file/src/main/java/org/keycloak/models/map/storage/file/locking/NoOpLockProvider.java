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
 * A {@link FileLockProvider} that creates {@link NoOpLock}s.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class NoOpLockProvider implements FileLockProvider {

    @Override
    public Lock createLock(final String areaName, final String realmId, final Path path) {
        return NoOpLock.getInstance();
    }

    @Override
    public void releaseAllLocks() {
        // no-op
    }
}

/**
 * A singleton {@link Lock} implementation that is no-op - i.e. both {@link #acquire()} and {@link #release()} implementations
 * do nothing.
 */
class NoOpLock implements Lock {

    private static NoOpLock instance = new NoOpLock();

    private NoOpLock() {}

    public static NoOpLock getInstance() {
        return instance;
    }

    @Override
    public Lock acquire() {
        // no-op
        return this;
    }

    @Override
    public Lock release() {
        // no-op
        return this;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}