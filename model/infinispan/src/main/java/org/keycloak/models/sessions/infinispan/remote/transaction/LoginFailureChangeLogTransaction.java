/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.remote.transaction;

import org.keycloak.models.sessions.infinispan.changes.remote.remover.query.ByRealmIdQueryConditionalRemover;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.UpdaterFactory;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.loginfailures.LoginFailuresUpdater;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;

/**
 * Syntactic sugar for
 * {@code RemoteChangeLogTransaction<LoginFailureKey, LoginFailureEntity, LoginFailuresUpdater,
 * ByRealmIdConditionalRemover<LoginFailureKey, LoginFailureEntity>>}
 */
public class LoginFailureChangeLogTransaction extends RemoteChangeLogTransaction<LoginFailureKey, LoginFailureEntity, LoginFailuresUpdater, ByRealmIdQueryConditionalRemover<LoginFailureKey, LoginFailureEntity>> {

    public LoginFailureChangeLogTransaction(UpdaterFactory<LoginFailureKey, LoginFailureEntity, LoginFailuresUpdater> factory, SharedState<LoginFailureKey, LoginFailureEntity> sharedState, ByRealmIdQueryConditionalRemover<LoginFailureKey, LoginFailureEntity> conditionalRemover) {
        super(factory, sharedState, conditionalRemover);
    }

    public void removeByRealmId(String realmId) {
        getConditionalRemover().removeByRealmId(realmId);
    }
}
