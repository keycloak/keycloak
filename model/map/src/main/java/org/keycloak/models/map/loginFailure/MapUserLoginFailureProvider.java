/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.loginFailure;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.map.common.Serialization;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;

import java.util.UUID;
import java.util.function.Function;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserLoginFailureProvider implements UserLoginFailureProvider {

    private static final Logger LOG = Logger.getLogger(MapUserLoginFailureProvider.class);
    private final KeycloakSession session;
    protected final MapKeycloakTransaction<UUID, MapUserLoginFailureEntity, UserLoginFailureModel> userLoginFailureTx;
    private final MapStorage<UUID, MapUserLoginFailureEntity, UserLoginFailureModel> userLoginFailureStore;

    public MapUserLoginFailureProvider(KeycloakSession session, MapStorage<UUID, MapUserLoginFailureEntity, UserLoginFailureModel> userLoginFailureStore) {
        this.session = session;
        this.userLoginFailureStore = userLoginFailureStore;

        userLoginFailureTx = userLoginFailureStore.createTransaction(session);
        session.getTransactionManager().enlistAfterCompletion(userLoginFailureTx);
    }

    private Function<MapUserLoginFailureEntity, UserLoginFailureModel> userLoginFailureEntityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapUserLoginFailureAdapter(session, realm, registerEntityForChanges(origEntity));
    }

    private MapUserLoginFailureEntity registerEntityForChanges(MapUserLoginFailureEntity origEntity) {
        MapUserLoginFailureEntity res = userLoginFailureTx.read(origEntity.getId(), id -> Serialization.from(origEntity));
        userLoginFailureTx.updateIfChanged(origEntity.getId(), res, MapUserLoginFailureEntity::isUpdated);
        return res;
    }

    @Override
    public UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId) {
        ModelCriteriaBuilder<UserLoginFailureModel> mcb = userLoginFailureStore.createCriteriaBuilder()
                .compare(UserLoginFailureModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(UserLoginFailureModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, userId);

        LOG.tracef("getUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        return userLoginFailureTx.getUpdatedNotRemoved(mcb)
                .findFirst()
                .map(userLoginFailureEntityToAdapterFunc(realm))
                .orElse(null);
    }

    @Override
    public UserLoginFailureModel addUserLoginFailure(RealmModel realm, String userId) {
        ModelCriteriaBuilder<UserLoginFailureModel> mcb = userLoginFailureStore.createCriteriaBuilder()
                .compare(UserLoginFailureModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(UserLoginFailureModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, userId);

        LOG.tracef("addUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        MapUserLoginFailureEntity userLoginFailureEntity = userLoginFailureTx.getUpdatedNotRemoved(mcb).findFirst().orElse(null);

        if (userLoginFailureEntity == null) {
            userLoginFailureEntity = new MapUserLoginFailureEntity(UUID.randomUUID(), realm.getId(), userId);

            userLoginFailureTx.create(userLoginFailureEntity.getId(), userLoginFailureEntity);
        }

        return userLoginFailureEntityToAdapterFunc(realm).apply(userLoginFailureEntity);
    }

    @Override
    public void removeUserLoginFailure(RealmModel realm, String userId) {
        ModelCriteriaBuilder<UserLoginFailureModel> mcb = userLoginFailureStore.createCriteriaBuilder()
                .compare(UserLoginFailureModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(UserLoginFailureModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, userId);

        LOG.tracef("removeUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        userLoginFailureTx.delete(UUID.randomUUID(), mcb);
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        ModelCriteriaBuilder<UserLoginFailureModel> mcb = userLoginFailureStore.createCriteriaBuilder()
                .compare(UserLoginFailureModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId());

        LOG.tracef("removeAllUserLoginFailures(%s)%s", realm, getShortStackTrace());

        userLoginFailureTx.delete(UUID.randomUUID(), mcb);
    }

    @Override
    public void close() {

    }
}
