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
package org.keycloak.models.map.storage.jpa.authorization.scope;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder.In;

import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.model.Scope.SearchableFields;
import org.keycloak.models.map.common.StringKeyConverter.UUIDKey;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaPredicateFunction;
import org.keycloak.models.map.storage.jpa.authorization.scope.entity.JpaScopeEntity;
import org.keycloak.storage.SearchableModelField;

public class JpaScopeModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaScopeEntity, Scope, JpaScopeModelCriteriaBuilder> {

    public JpaScopeModelCriteriaBuilder() {
        super(JpaScopeModelCriteriaBuilder::new);
    }

    private JpaScopeModelCriteriaBuilder(JpaPredicateFunction<JpaScopeEntity> predicateFunc) {
        super(JpaScopeModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaScopeModelCriteriaBuilder compare(SearchableModelField<? super Scope> modelField, ModelCriteriaBuilder.Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == SearchableFields.ID ||
                    modelField == SearchableFields.RESOURCE_SERVER_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaScopeModelCriteriaBuilder((cb, query, root) -> {
                        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(Objects.toString(value[0], null));
                        if (uuid == null) return cb.or();
                        return cb.equal(root.get(modelField.getName()), uuid);
                    });
                } else if (modelField == SearchableFields.REALM_ID ||
                           modelField == SearchableFields.NAME) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaScopeModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case ILIKE:
                if (modelField == SearchableFields.NAME) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaScopeModelCriteriaBuilder((cb, query, root) ->
                        cb.like(cb.lower(root.get(modelField.getName())), value[0].toString().toLowerCase())
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case IN:
                if (modelField == SearchableFields.ID) {

                    final Collection<?> collectionValues = getValuesForInOperator(value, modelField);

                    if (collectionValues.isEmpty()) {
                        return new JpaScopeModelCriteriaBuilder((cb, query, root) -> cb.or());
                    }

                    return new JpaScopeModelCriteriaBuilder((cb, query, root) ->  {
                        In<UUID> in = cb.in(root.get("id"));
                        for (Object id : collectionValues) {
                            try {
                                in.value(UUIDKey.INSTANCE.fromString(Objects.toString(id, null)));
                            } catch (IllegalArgumentException e) {
                                throw new CriterionNotSupportedException(modelField, op, id + " id is not in uuid format.", e);
                            }
                        }
                        return in;
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }

}
