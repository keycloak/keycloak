/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.concurrent.TimeUnit;

import org.keycloak.models.RealmModel;

/**
 * <p>Shared methods to calculate login failure idle times.</p>
 */
public class LoginFailureUtils {

    /**
     * Compute the expiration time cut-off in milliseconds for expiring login failure entries.
     *
     * @param realm current realm
     * @param currentTime current timestamp in seconds since last epoch
     * @return Timestamp in milliseconds, or -1L if the realm will never expire.
     */
    public static long computeExpirationCutOffTimestamp(RealmModel realm, long currentTime) {
        if (realm.isPermanentLockout() && realm.getMaxTemporaryLockouts() == 0) {
            // If mode is permanent lockout only, the "failure reset time" cannot be configured and login failures should never expire.
            return -1L;
        }
        // expired if last-failure + max-delta-time < current time
        return TimeUnit.SECONDS.toMillis(currentTime - realm.getMaxDeltaTimeSeconds());
    }

}
