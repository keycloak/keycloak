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

package org.keycloak.models.map.storage.hotRod.locking;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.models.locking.LockAcquiringTimeoutException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class HotRodLocksUtils {

    public static final String SEPARATOR = ";";
    private static final String INSTANCE_IDENTIFIER = getKeycloakInstanceIdentifier();

    /**
     * Repeatedly attempts to put an entry with the key {@code lockName}
     * to the {@code locksCache}. Succeeds only if there is no entry with
     * the same key already.
     * <p/>
     * Execution of this method is time bounded, if this method does not
     * succeed within {@code timeoutMilliseconds} it gives up and returns
     * false.
     * <p/>
     * There is a pause after each unsuccessful attempt equal to
     * {@code repeatInterval} milliseconds
     *
     * @param locksCache     Cache that will be used for putting the value
     * @param lockName       Name of the entry
     * @param timeout        duration to wait until the lock is acquired
     * @param repeatInterval Number of milliseconds to wait after each
     *                       unsuccessful attempt
     * @throws LockAcquiringTimeoutException the key {@code lockName} was NOT put into the {@code map}
     *                                       within time boundaries
     * @throws IllegalStateException         when a {@code lock} value found in the storage has wrong format. It is expected
     *                                       the lock value has the following format {@code 'timeAcquired;keycloakInstanceIdentifier'}
     */
    public static void repeatPutIfAbsent(RemoteCache<String, String> locksCache, String lockName, Duration timeout, int repeatInterval) throws LockAcquiringTimeoutException {
        final AtomicReference<String> currentOwnerRef = new AtomicReference<>(null);
        try {
            Retry.executeWithBackoff(i -> {
                String curr = locksCache.withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(lockName, Time.currentTimeMillis() + SEPARATOR + INSTANCE_IDENTIFIER);
                currentOwnerRef.set(curr);
                if (curr != null) {
                    throw new AssertionError("Acquiring lock in iteration " + i + " was not successful");
                }
            }, timeout, repeatInterval);
        } catch (AssertionError ex) {
            String currentOwner = currentOwnerRef.get();
            String[] split = currentOwner == null ? null : currentOwner.split(SEPARATOR, 2);
            if (currentOwner == null || split.length != 2) throw new IllegalStateException("Bad lock value format found in storage for lock " + lockName + ". " +
                    "It is expected the format to be 'timeAcquired;keycloakInstanceIdentifier' but was " + currentOwner);
            throw new LockAcquiringTimeoutException(lockName, split[1], Instant.ofEpochMilli(Long.parseLong(split[0])));
        }
    }

    private static String getKeycloakInstanceIdentifier() {
        long pid = ProcessHandle.current().pid();
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
        }

        return pid + "@" + hostname;
    }

    /**
     * Removes the entry with key {@code lockName} from map if the value
     * of the entry is equal to this node's identifier
     *
     * @param map Map that will be used for removing
     * @param lockName Name of the entry
     * @return true if the entry was removed, false otherwise
     */
    public static boolean removeWithInstanceIdentifier(ConcurrentMap<String, String> map, String lockName) {
        String value = map.get(lockName);
        if (value != null && value.endsWith(INSTANCE_IDENTIFIER)) {
            map.remove(lockName);
            return true;
        } else {
            return false;
        }
    }
}
