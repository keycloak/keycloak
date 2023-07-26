/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.lock;

import org.keycloak.models.map.common.StringKeyConverter.UUIDKey;
import org.keycloak.models.map.lock.MapLockEntity;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaPredicateFunction;
import org.keycloak.models.map.storage.jpa.authorization.resource.entity.JpaResourceEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.Objects;
import java.util.UUID;

import static org.keycloak.models.map.lock.MapLockEntity.SearchableFields;

public class JpaLockModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaResourceEntity, MapLockEntity, JpaLockModelCriteriaBuilder> {

    public JpaLockModelCriteriaBuilder() {
        super(JpaLockModelCriteriaBuilder::new);
    }

    private JpaLockModelCriteriaBuilder(JpaPredicateFunction<JpaResourceEntity> predicateFunc) {
        super(JpaLockModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaLockModelCriteriaBuilder compare(SearchableModelField<? super MapLockEntity> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == SearchableFields.NAME) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaLockModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }

}
