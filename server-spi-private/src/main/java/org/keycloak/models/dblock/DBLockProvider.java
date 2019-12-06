/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.dblock;

import org.keycloak.provider.Provider;

/**
 * <p>Global database lock to ensure that some actions in DB can be done just be
 * one cluster node at a time.</p>
 *
 * <p>There are different namespaces that can be locked. The same <em>DBLockProvider</em>
 * (same session in keycloack) can only be used to lock one namespace, a second
 * attempt will throw a <em>RuntimeException</em>. The <em>hasLock</em> method
 * returns the local namespace locked by this provider.</p>
 *
 * <p>Different <em>DBLockProvider</em> instances can be used to lock in
 * different threads. Note that the <em>DBLockProvider</em> is associated to
 * the session (so in order to have different lock providers different sessions
 * are needed).</p>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface DBLockProvider extends Provider {

    /**
     * Lock namespace to have different lock types or contexts.
     */
    enum Namespace {

        DATABASE(1),
        KEYCLOAK_BOOT(1000),
        OFFLINE_SESSIONS(1001);

        private final int id;

        Namespace(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    };

    /**
     * Try to retrieve DB lock or wait if retrieve was unsuccessful.
     * Throw exception if lock can't be retrieved within specified timeout (900 seconds by default)
     * Throw exception if a different namespace has already been locked by this provider.
     *
     * @param lock The namespace to lock
     */
    void waitForLock(Namespace lock);

    /**
     * Release previously acquired lock by this provider.
     */
    void releaseLock();

    /**
     * Returns the current provider namespace locked or null
     *
     * @return The namespace locked or null if there is no lock
     */
    Namespace getCurrentLock();

    /**
     * @return true if provider supports forced unlock at startup
     */
    boolean supportsForcedUnlock();

    /**
     * Will destroy whole state of DB lock (drop table/collection to track locking).
     * */
    void destroyLockInfo();
}
