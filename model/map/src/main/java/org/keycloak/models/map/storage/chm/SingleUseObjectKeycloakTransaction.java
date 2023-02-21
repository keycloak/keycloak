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

import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.Map;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class SingleUseObjectKeycloakTransaction<K> extends ConcurrentHashMapKeycloakTransaction<K, MapSingleUseObjectEntity, SingleUseObjectValueModel> {

    public SingleUseObjectKeycloakTransaction(ConcurrentHashMapCrudOperations<MapSingleUseObjectEntity, SingleUseObjectValueModel> map,
                                              StringKeyConverter<K> keyConverter,
                                              DeepCloner cloner,
                                              Map<SearchableModelField<? super SingleUseObjectValueModel>,
                                                    MapModelCriteriaBuilder.UpdatePredicatesFunc<K, MapSingleUseObjectEntity, SingleUseObjectValueModel>> fieldPredicates) {
        super(map, keyConverter, cloner, fieldPredicates);
    }

    @Override
    public MapSingleUseObjectEntity create(MapSingleUseObjectEntity value) {
        if (value.getId() == null) {
            if (value.getObjectKey() != null) {
                value.setId(value.getObjectKey());
            }
        }
        return super.create(value);
    }
}
