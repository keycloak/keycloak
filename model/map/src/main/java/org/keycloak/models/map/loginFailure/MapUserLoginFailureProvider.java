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

import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import java.util.function.Function;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserLoginFailureProvider implements UserLoginFailureProvider {

    private static final Logger LOG = Logger.getLogger(MapUserLoginFailureProvider.class);
    private final KeycloakSession session;
    protected final MapKeycloakTransaction<MapUserLoginFailureEntity, UserLoginFailureModel> userLoginFailureTx;

    public MapUserLoginFailureProvider(KeycloakSession session, MapStorage<MapUserLoginFailureEntity, UserLoginFailureModel> userLoginFailureStore) {
        this.session = session;

        userLoginFailureTx = userLoginFailureStore.createTransaction(session);
        session.getTransactionManager().enlistAfterCompletion(userLoginFailureTx);
    }

    private Function<MapUserLoginFailureEntity, UserLoginFailureModel> userLoginFailureEntityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapUserLoginFailureAdapter(session, realm, origEntity);
    }

    @Override
    public UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId) {
        DefaultModelCriteria<UserLoginFailureModel> mcb = criteria();
        mcb = mcb.compare(UserLoginFailureModel.SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(UserLoginFailureModel.SearchableFields.USER_ID, Operator.EQ, userId);

        LOG.tracef("getUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        return userLoginFailureTx.read(withCriteria(mcb))
                .findFirst()
                .map(userLoginFailureEntityToAdapterFunc(realm))
                .orElse(null);
    }

    @Override
    public UserLoginFailureModel addUserLoginFailure(RealmModel realm, String userId) {
        DefaultModelCriteria<UserLoginFailureModel> mcb = criteria();
        mcb = mcb.compare(UserLoginFailureModel.SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(UserLoginFailureModel.SearchableFields.USER_ID, Operator.EQ, userId);

        LOG.tracef("addUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        MapUserLoginFailureEntity userLoginFailureEntity = userLoginFailureTx.read(withCriteria(mcb)).findFirst().orElse(null);

        if (userLoginFailureEntity == null) {
            userLoginFailureEntity = new MapUserLoginFailureEntityImpl();
            userLoginFailureEntity.setRealmId(realm.getId());
            userLoginFailureEntity.setUserId(userId);

            userLoginFailureEntity = userLoginFailureTx.create(userLoginFailureEntity);
        }

        return userLoginFailureEntityToAdapterFunc(realm).apply(userLoginFailureEntity);
    }

    @Override
    public void removeUserLoginFailure(RealmModel realm, String userId) {
        DefaultModelCriteria<UserLoginFailureModel> mcb = criteria();
        mcb = mcb.compare(UserLoginFailureModel.SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(UserLoginFailureModel.SearchableFields.USER_ID, Operator.EQ, userId);

        LOG.tracef("removeUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        userLoginFailureTx.delete(withCriteria(mcb));
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        DefaultModelCriteria<UserLoginFailureModel> mcb = criteria();
        mcb = mcb.compare(UserLoginFailureModel.SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        LOG.tracef("removeAllUserLoginFailures(%s)%s", realm, getShortStackTrace());

        userLoginFailureTx.delete(withCriteria(mcb));
    }

    @Override
    public void close() {

    }
}
