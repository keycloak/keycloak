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
