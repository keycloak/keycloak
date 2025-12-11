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

package org.keycloak.models.sessions.infinispan.entities;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * Represents an entity containing data about a session, i.e. an object that is stored in infinispan cache.
 * Due to conflict management in {@code InfinispanChangelogBasedTransaction} that use Infinispan's {@code replace()}
 * method, overriding {@link #hashCode()} and {@link #equals(java.lang.Object)} is <b>mandatory</b> in descendants.
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class SessionEntity {

    private String realmId;
    private boolean isOffline;

    /**
     * Returns realmId ID.
     * @return
     */
    @ProtoField(1)
    @Basic
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public SessionEntity() {
    }

    protected SessionEntity(String realmId) {
        this.realmId = realmId;
    }

    @Deprecated(since = "26.4", forRemoval = true)
    //no longer used
    public SessionEntityWrapper mergeRemoteEntityWithLocalEntity(SessionEntityWrapper localEntityWrapper) {
        if (localEntityWrapper == null) {
            return new SessionEntityWrapper<>(this);
        } else {
            return new SessionEntityWrapper<>(localEntityWrapper.getLocalMetadata(), this);
        }
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    public boolean isOffline() {
        if (!MultiSiteUtils.isPersistentSessionsEnabled()) {
            throw new IllegalArgumentException("Offline flags are not supported in non-persistent-session environments.");
        }
        return isOffline;
    }

    public void setOffline(boolean offline) {
        if (!MultiSiteUtils.isPersistentSessionsEnabled()) {
            throw new IllegalArgumentException("Offline flags are not supported in non-persistent-session environments.");
        }
        isOffline = offline;
    }

    public boolean shouldEvaluateRemoval() {
        return false;
    }
}
