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

package org.keycloak.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.PrioritizedComponentModel;

/**
 * Stored configuration of a User Storage provider instance.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class UserStorageProviderModel extends PrioritizedComponentModel {

    public static final String CACHE_POLICY = "cachePolicy";
    public static final String MAX_LIFESPAN = "maxLifespan";
    public static final String EVICTION_HOUR = "evictionHour";
    public static final String EVICTION_MINUTE = "evictionMinute";
    public static final String EVICTION_DAY = "evictionDay";
    public static final String CACHE_INVALID_BEFORE = "cacheInvalidBefore";
    public static final String IMPORT_ENABLED = "importEnabled";
    public static final String FULL_SYNC_PERIOD = "fullSyncPeriod";
    public static final String CHANGED_SYNC_PERIOD = "changedSyncPeriod";
    public static final String LAST_SYNC = "lastSync";

    public static enum CachePolicy {
        NO_CACHE,
        DEFAULT,
        EVICT_DAILY,
        EVICT_WEEKLY,
        MAX_LIFESPAN
    }

    public UserStorageProviderModel() {
        setProviderType(UserStorageProvider.class.getName());
    }

    public UserStorageProviderModel(ComponentModel copy) {
        super(copy);
    }

    private transient Integer fullSyncPeriod;
    private transient Integer changedSyncPeriod;
    private transient Integer lastSync;
    private transient Boolean importEnabled;
    private transient CachePolicy cachePolicy;
    private transient long maxLifespan = -1;
    private transient int evictionHour = -1;
    private transient int evictionMinute = -1;
    private transient int evictionDay = -1;
    private transient long cacheInvalidBefore = -1;

    public CachePolicy getCachePolicy() {
        if (cachePolicy == null) {
            String str = getConfig().getFirst(CACHE_POLICY);
            if (str == null) return null;
            cachePolicy = CachePolicy.valueOf(str);
        }
        return cachePolicy;
    }

    public void setCachePolicy(CachePolicy cachePolicy) {
        this.cachePolicy = cachePolicy;
        if (cachePolicy == null) {
            getConfig().remove(CACHE_POLICY);

        } else {
            getConfig().putSingle(CACHE_POLICY, cachePolicy.name());
        }
    }

    public long getMaxLifespan() {
        if (maxLifespan < 0) {
            String str = getConfig().getFirst(MAX_LIFESPAN);
            if (str == null) return -1;
            maxLifespan = Long.valueOf(str);
        }
        return maxLifespan;
    }

    public void setMaxLifespan(long maxLifespan) {
        this.maxLifespan = maxLifespan;
        getConfig().putSingle(MAX_LIFESPAN, Long.toString(maxLifespan));
    }

    public int getEvictionHour() {
        if (evictionHour < 0) {
            String str = getConfig().getFirst(EVICTION_HOUR);
            if (str == null) return -1;
            evictionHour = Integer.valueOf(str);
        }
        return evictionHour;
    }

    public void setEvictionHour(int evictionHour) {
        if (evictionHour > 23 || evictionHour < 0) throw new IllegalArgumentException("Must be between 0 and 23");
        this.evictionHour = evictionHour;
        getConfig().putSingle(EVICTION_HOUR, Integer.toString(evictionHour));
    }

    public int getEvictionMinute() {
        if (evictionMinute < 0) {
            String str = getConfig().getFirst(EVICTION_MINUTE);
            if (str == null) return -1;
            evictionMinute = Integer.valueOf(str);
        }
        return evictionMinute;
    }

    public void setEvictionMinute(int evictionMinute) {
        if (evictionMinute > 59 || evictionMinute < 0) throw new IllegalArgumentException("Must be between 0 and 59");
        this.evictionMinute = evictionMinute;
        getConfig().putSingle(EVICTION_MINUTE, Integer.toString(evictionMinute));
    }

    public int getEvictionDay() {
        if (evictionDay < 0) {
            String str = getConfig().getFirst(EVICTION_DAY);
            if (str == null) return -1;
            evictionDay = Integer.valueOf(str);
        }
        return evictionDay;
    }

    public void setEvictionDay(int evictionDay) {
        if (evictionDay > 7 || evictionDay < 1) throw new IllegalArgumentException("Must be between 1 and 7");
        this.evictionDay = evictionDay;
        getConfig().putSingle(EVICTION_DAY, Integer.toString(evictionDay));
    }

    public long getCacheInvalidBefore() {
        if (cacheInvalidBefore < 0) {
            String str = getConfig().getFirst(CACHE_INVALID_BEFORE);
            if (str == null) return -1;
            cacheInvalidBefore = Long.valueOf(str);
        }
        return cacheInvalidBefore;
    }

    public void setCacheInvalidBefore(long cacheInvalidBefore) {
        this.cacheInvalidBefore = cacheInvalidBefore;
        getConfig().putSingle(CACHE_INVALID_BEFORE, Long.toString(cacheInvalidBefore));
    }

    public boolean isImportEnabled() {
        if (importEnabled == null) {
            String val = getConfig().getFirst(IMPORT_ENABLED);
            if (val == null) {
                importEnabled = true;
            } else {
                importEnabled = Boolean.valueOf(val);
            }
        }
        return importEnabled;

    }



    public void setImportEnabled(boolean flag) {
        importEnabled = flag;
        getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(flag));
    }

    public int getFullSyncPeriod() {
        if (fullSyncPeriod == null) {
            String val = getConfig().getFirst(FULL_SYNC_PERIOD);
            if (val == null) {
                fullSyncPeriod = -1;
            } else {
                fullSyncPeriod = Integer.valueOf(val);
            }
        }
        return fullSyncPeriod;
    }

    public void setFullSyncPeriod(int fullSyncPeriod) {
        this.fullSyncPeriod = fullSyncPeriod;
        getConfig().putSingle(FULL_SYNC_PERIOD, Integer.toString(fullSyncPeriod));
    }

    public int getChangedSyncPeriod() {
        if (changedSyncPeriod == null) {
            String val = getConfig().getFirst(CHANGED_SYNC_PERIOD);
            if (val == null) {
                changedSyncPeriod = -1;
            } else {
                changedSyncPeriod = Integer.valueOf(val);
            }
        }
        return changedSyncPeriod;
    }

    public void setChangedSyncPeriod(int changedSyncPeriod) {
        this.changedSyncPeriod = changedSyncPeriod;
        getConfig().putSingle(CHANGED_SYNC_PERIOD, Integer.toString(changedSyncPeriod));
    }

    public int getLastSync() {
        if (lastSync == null) {
            String val = getConfig().getFirst(LAST_SYNC);
            if (val == null) {
                lastSync = 0;
            } else {
                lastSync = Integer.valueOf(val);
            }
        }
        return lastSync;
    }

    public void setLastSync(int lastSync) {
        this.lastSync = lastSync;
        getConfig().putSingle(LAST_SYNC, Integer.toString(lastSync));
    }
}
