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
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;

import java.util.function.Function;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.MapStorageUtils.registerEntityForChanges;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserLoginFailureProvider<K> implements UserLoginFailureProvider {

    private static final Logger LOG = Logger.getLogger(MapUserLoginFailureProvider.class);
    private final KeycloakSession session;
    protected final MapKeycloakTransaction<K, MapUserLoginFailureEntity<K>, UserLoginFailureModel> userLoginFailureTx;
    private final MapStorage<K, MapUserLoginFailureEntity<K>, UserLoginFailureModel> userLoginFailureStore;

    public MapUserLoginFailureProvider(KeycloakSession session, MapStorage<K, MapUserLoginFailureEntity<K>, UserLoginFailureModel> userLoginFailureStore) {
        this.session = session;
        this.userLoginFailureStore = userLoginFailureStore;

        userLoginFailureTx = userLoginFailureStore.createTransaction(session);
        session.getTransactionManager().enlistAfterCompletion(userLoginFailureTx);
    }

    private Function<MapUserLoginFailureEntity<K>, UserLoginFailureModel> userLoginFailureEntityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapUserLoginFailureAdapter<K>(session, realm, registerEntityForChanges(userLoginFailureTx, origEntity)) {
            @Override
            public String getId() {
                return userLoginFailureStore.getKeyConvertor().keyToString(entity.getId());
            }
        };
    }

    @Override
    public UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId) {
        ModelCriteriaBuilder<UserLoginFailureModel> mcb = userLoginFailureStore.createCriteriaBuilder()
                .compare(UserLoginFailureModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(UserLoginFailureModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, userId);

        LOG.tracef("getUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        return userLoginFailureTx.read(withCriteria(mcb))
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

        MapUserLoginFailureEntity<K> userLoginFailureEntity = userLoginFailureTx.read(withCriteria(mcb)).findFirst().orElse(null);

        if (userLoginFailureEntity == null) {
            userLoginFailureEntity = new MapUserLoginFailureEntity<>(userLoginFailureStore.getKeyConvertor().yieldNewUniqueKey(), realm.getId(), userId);

            userLoginFailureTx.create(userLoginFailureEntity);
        }

        return userLoginFailureEntityToAdapterFunc(realm).apply(userLoginFailureEntity);
    }

    @Override
    public void removeUserLoginFailure(RealmModel realm, String userId) {
        ModelCriteriaBuilder<UserLoginFailureModel> mcb = userLoginFailureStore.createCriteriaBuilder()
                .compare(UserLoginFailureModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(UserLoginFailureModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, userId);

        LOG.tracef("removeUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        userLoginFailureTx.delete(userLoginFailureStore.getKeyConvertor().yieldNewUniqueKey(), withCriteria(mcb));
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        ModelCriteriaBuilder<UserLoginFailureModel> mcb = userLoginFailureStore.createCriteriaBuilder()
                .compare(UserLoginFailureModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId());

        LOG.tracef("removeAllUserLoginFailures(%s)%s", realm, getShortStackTrace());

        userLoginFailureTx.delete(userLoginFailureStore.getKeyConvertor().yieldNewUniqueKey(), withCriteria(mcb));
    }

    @Override
    public void close() {

    }
}
