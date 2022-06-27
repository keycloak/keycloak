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

import org.keycloak.models.ActionTokenValueModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

import java.util.stream.Stream;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class SingleUseObjectConcurrentHashMapStorage<K, V extends AbstractEntity, M> extends ConcurrentHashMapStorage<K, MapSingleUseObjectEntity, ActionTokenValueModel> {

    public SingleUseObjectConcurrentHashMapStorage(StringKeyConverter<K> keyConverter, DeepCloner cloner) {
        super(ActionTokenValueModel.class, keyConverter, cloner);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapKeycloakTransaction<MapSingleUseObjectEntity, ActionTokenValueModel> createTransaction(KeycloakSession session) {
        MapKeycloakTransaction<MapSingleUseObjectEntity, ActionTokenValueModel> actionTokenTransaction = session.getAttribute("map-transaction-" + hashCode(), MapKeycloakTransaction.class);

        if (actionTokenTransaction == null) {
            actionTokenTransaction = new SingleUseObjectKeycloakTransaction(this, keyConverter, cloner, fieldPredicates);
            session.setAttribute("map-transaction-" + hashCode(), actionTokenTransaction);
        }

        return actionTokenTransaction;
    }

    @Override
    public MapSingleUseObjectEntity create(MapSingleUseObjectEntity value) {
        if (value.getId() == null) {
            if (value.getUserId() != null && value.getActionId() != null && value.getActionVerificationNonce() != null) {
                value.setId(value.getUserId() + ":" + value.getActionId() + ":" + value.getActionVerificationNonce());
            }
            if (value.getObjectKey() != null) {
                value.setId(value.getObjectKey());
            }
        }
        return super.create(value);
    }

    @Override
    public Stream<MapSingleUseObjectEntity> read(QueryParameters<ActionTokenValueModel> queryParameters) {
        DefaultModelCriteria<ActionTokenValueModel> criteria = queryParameters.getModelCriteriaBuilder();

        if (criteria == null) {
            return Stream.empty();
        }

        SingleUseObjectModelCriteriaBuilder mcb = criteria.flashToModelCriteriaBuilder(createSingleUseObjectCriteriaBuilder());
        if (mcb.isValid()) {
            MapSingleUseObjectEntity value = read(mcb.getKey());
            return value != null ? Stream.of(value) : Stream.empty();
        }

        return super.read(queryParameters);
    }

    private SingleUseObjectModelCriteriaBuilder createSingleUseObjectCriteriaBuilder() {
        return new SingleUseObjectModelCriteriaBuilder();
    }

}
