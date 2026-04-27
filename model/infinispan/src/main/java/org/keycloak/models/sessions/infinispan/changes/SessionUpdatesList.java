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

package org.keycloak.models.sessions.infinispan.changes;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * tracks all changes to the underlying session in this transaction
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionUpdatesList<S extends SessionEntity> {

    private final RealmModel realm;

    private final ClientModel client;

    private final SessionEntityWrapper<S> entityWrapper;

    private List<SessionUpdateTask<S>> updateTasks = new LinkedList<>();

    private final UserSessionModel.SessionPersistenceState persistenceState;

    public SessionUpdatesList(RealmModel realm, SessionEntityWrapper<S> entityWrapper) {
        this(realm, entityWrapper, UserSessionModel.SessionPersistenceState.PERSISTENT);
    }

    public SessionUpdatesList(RealmModel realm, SessionEntityWrapper<S> entityWrapper, UserSessionModel.SessionPersistenceState persistenceState) {
        this.realm = realm;
        this.entityWrapper = entityWrapper;
        this.persistenceState = persistenceState;
        this.client = entityWrapper.getClientIfNeeded(realm);
    }

    public RealmModel getRealm() {
        return realm;
    }

    public ClientModel getClient() {
        return client;
    }

    public SessionEntityWrapper<S> getEntityWrapper() {
        return entityWrapper;
    }


    public void add(SessionUpdateTask<S> task) {
        updateTasks.add(task);
    }

    public List<SessionUpdateTask<S>> getUpdateTasks() {
        return updateTasks;
    }

    public void setUpdateTasks(List<SessionUpdateTask<S>> updateTasks) {
        this.updateTasks = updateTasks;
    }

    public UserSessionModel.SessionPersistenceState getPersistenceState() {
        return persistenceState;
    }

    public void addAndExecute(SessionUpdateTask<S> task) {
        add(task);
        task.runUpdate(getEntityWrapper().getEntity());
    }
}
