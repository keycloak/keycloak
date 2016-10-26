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
            String str = getConfig().getFirst("cachePolicy");
            if (str == null) return null;
            cachePolicy = CachePolicy.valueOf(str);
        }
        return cachePolicy;
    }

    public void setCachePolicy(CachePolicy cachePolicy) {
        this.cachePolicy = cachePolicy;
        if (cachePolicy == null) {
            getConfig().remove("cachePolicy");

        } else {
            getConfig().putSingle("cachePolicy", cachePolicy.name());
        }
    }

    public long getMaxLifespan() {
        if (maxLifespan < 0) {
            String str = getConfig().getFirst("maxLifespan");
            if (str == null) return -1;
            maxLifespan = Long.valueOf(str);
        }
        return maxLifespan;
    }

    public void setMaxLifespan(long maxLifespan) {
        this.maxLifespan = maxLifespan;
        getConfig().putSingle("maxLifespan", Long.toString(maxLifespan));
    }

    public int getEvictionHour() {
        if (evictionHour < 0) {
            String str = getConfig().getFirst("evictionHour");
            if (str == null) return -1;
            evictionHour = Integer.valueOf(str);
        }
        return evictionHour;
    }

    public void setEvictionHour(int evictionHour) {
        if (evictionHour > 23 || evictionHour < 0) throw new IllegalArgumentException("Must be between 0 and 23");
        this.evictionHour = evictionHour;
        getConfig().putSingle("evictionHour", Integer.toString(evictionHour));
    }

    public int getEvictionMinute() {
        if (evictionMinute < 0) {
            String str = getConfig().getFirst("evictionMinute");
            if (str == null) return -1;
            evictionMinute = Integer.valueOf(str);
        }
        return evictionMinute;
    }

    public void setEvictionMinute(int evictionMinute) {
        if (evictionMinute > 59 || evictionMinute < 0) throw new IllegalArgumentException("Must be between 0 and 59");
        this.evictionMinute = evictionMinute;
        getConfig().putSingle("evictionMinute", Integer.toString(evictionMinute));
    }

    public int getEvictionDay() {
        if (evictionDay < 0) {
            String str = getConfig().getFirst("evictionDay");
            if (str == null) return -1;
            evictionDay = Integer.valueOf(str);
        }
        return evictionDay;
    }

    public void setEvictionDay(int evictionDay) {
        if (evictionDay > 7 || evictionDay < 1) throw new IllegalArgumentException("Must be between 1 and 7");
        this.evictionDay = evictionDay;
        getConfig().putSingle("evictionDay", Integer.toString(evictionDay));
    }

    public long getCacheInvalidBefore() {
        if (cacheInvalidBefore < 0) {
            String str = getConfig().getFirst("cacheInvalidBefore");
            if (str == null) return -1;
            cacheInvalidBefore = Long.valueOf(str);
        }
        return cacheInvalidBefore;
    }

    public void setCacheInvalidBefore(long cacheInvalidBefore) {
        this.cacheInvalidBefore = cacheInvalidBefore;
        getConfig().putSingle("cacheInvalidBefore", Long.toString(cacheInvalidBefore));
    }

    public boolean isImportEnabled() {
        if (importEnabled == null) {
            String val = getConfig().getFirst("importEnabled");
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
        getConfig().putSingle("importEnabled", Boolean.toString(flag));
    }

    public int getFullSyncPeriod() {
        if (fullSyncPeriod == null) {
            String val = getConfig().getFirst("fullSyncPeriod");
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
        getConfig().putSingle("fullSyncPeriod", Integer.toString(fullSyncPeriod));
    }

    public int getChangedSyncPeriod() {
        if (changedSyncPeriod == null) {
            String val = getConfig().getFirst("changedSyncPeriod");
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
        getConfig().putSingle("changedSyncPeriod", Integer.toString(changedSyncPeriod));
    }

    public int getLastSync() {
        if (lastSync == null) {
            String val = getConfig().getFirst("lastSync");
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
        getConfig().putSingle("lastSync", Integer.toString(lastSync));
    }
}
