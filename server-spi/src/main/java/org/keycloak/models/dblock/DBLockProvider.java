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
 * Global database lock to ensure that some actions in DB can be done just be one cluster node at a time.
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface DBLockProvider extends Provider {


    /**
     * Try to retrieve DB lock or wait if retrieve was unsuccessful. Throw exception if lock can't be retrieved within specified timeout (900 seconds by default)
     */
    void waitForLock();


    /**
     * Release previously acquired lock
     */
    void releaseLock();

    /**
     * Check if I have lock
     *
     * @return
     */
    boolean hasLock();


    /**
     * @return true if provider supports forced unlock at startup
     */
    boolean supportsForcedUnlock();


    /**
     * Will destroy whole state of DB lock (drop table/collection to track locking).
     * */
    void destroyLockInfo();
}
