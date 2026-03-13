/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
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

package org.keycloak.models.redis.entities;

/**
 * Entity representing user login failure data stored in Redis for brute force protection.
 */
public class RedisLoginFailureEntity {

    private String realmId;
    private String odString;
    private int failedLoginNotBefore;
    private int numFailures;
    private int numTemporaryLockouts;
    private long lastFailure;
    private String lastIPFailure;

    public RedisLoginFailureEntity() {}

    public RedisLoginFailureEntity(String realmId, String odString) {
        this.realmId = realmId;
        this.userId = odString;
    }

    public String getRealmId() { return realmId; }
    public void setRealmId(String realmId) { this.realmId = realmId; }

    public String getUserId() { return userId; }
    public void setUserId(String odString) { this.userId = odString; }

    public int getFailedLoginNotBefore() { return failedLoginNotBefore; }
    public void setFailedLoginNotBefore(int failedLoginNotBefore) { this.failedLoginNotBefore = failedLoginNotBefore; }

    public int getNumFailures() { return numFailures; }
    public void setNumFailures(int numFailures) { this.numFailures = numFailures; }

    public int getNumTemporaryLockouts() { return numTemporaryLockouts; }
    public void setNumTemporaryLockouts(int numTemporaryLockouts) { this.numTemporaryLockouts = numTemporaryLockouts; }

    public long getLastFailure() { return lastFailure; }
    public void setLastFailure(long lastFailure) { this.lastFailure = lastFailure; }

    public String getLastIPFailure() { return lastIPFailure; }
    public void setLastIPFailure(String lastIPFailure) { this.lastIPFailure = lastIPFailure; }

    public void clearFailures() {
        this.numFailures = 0;
        this.lastFailure = 0;
        this.lastIPFailure = null;
    }

    public static String createKey(String realmId, String odString) {
        return realmId + ":" + odString;
    }

    private String userId;
}
