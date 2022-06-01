/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.chm;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.models.map.storage.QueryParameters.withCriteria;

public class UserSessionCascadeRemovalTransaction<K, V extends MapUserSessionEntity> extends ConcurrentHashMapKeycloakTransaction<K, V, UserSessionModel> {

    private final MapKeycloakTransaction<? extends MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionTr;

    public UserSessionCascadeRemovalTransaction(ConcurrentHashMapCrudOperations<V, UserSessionModel> userSessionConcurrentHashMapStorage,
                                                MapKeycloakTransaction<? extends MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionTr,
                                                StringKeyConverter<K> keyConverter,
                                                DeepCloner cloner,
                                                Map<SearchableModelField<? super UserSessionModel>,
                                                   MapModelCriteriaBuilder.UpdatePredicatesFunc<K,
                                                           V,
                                                           UserSessionModel>> fieldPredicates) {
        super(userSessionConcurrentHashMapStorage, keyConverter, cloner, fieldPredicates);
        this.clientSessionTr = clientSessionTr;
    }

    @Override
    public long delete(QueryParameters<UserSessionModel> queryParameters) {
        Set<String> ids = read(queryParameters).map(AbstractEntity::getId).collect(Collectors.toSet());
        DefaultModelCriteria<AuthenticatedClientSessionModel> csMcb = DefaultModelCriteria.<AuthenticatedClientSessionModel>criteria()
                .compare(AuthenticatedClientSessionModel.SearchableFields.USER_SESSION_ID, ModelCriteriaBuilder.Operator.IN, ids);
        clientSessionTr.delete(withCriteria(csMcb));
        return super.delete(queryParameters);
    }

    @Override
    public boolean delete(String key) {
        DefaultModelCriteria<AuthenticatedClientSessionModel> csMcb = DefaultModelCriteria.<AuthenticatedClientSessionModel>criteria()
                .compare(AuthenticatedClientSessionModel.SearchableFields.USER_SESSION_ID, ModelCriteriaBuilder.Operator.EQ, key);
        clientSessionTr.delete(withCriteria(csMcb));
        return super.delete(key);
    }

}
