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
package org.keycloak.models.map.storage.jpa.client;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientModel.SearchableFields;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.client.entity.JpaClientEntity;
import org.keycloak.models.map.storage.jpa.client.entity.JpaClientAttributeEntity;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.role.JpaPredicateFunction;
import org.keycloak.storage.SearchableModelField;

public class JpaClientModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaClientEntity, ClientModel, JpaClientModelCriteriaBuilder> {

    public JpaClientModelCriteriaBuilder() {
        super(JpaClientModelCriteriaBuilder::new);
    }

    private JpaClientModelCriteriaBuilder(JpaPredicateFunction<JpaClientEntity> predicateFunc) {
        super(JpaClientModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaClientModelCriteriaBuilder compare(SearchableModelField<? super ClientModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == SearchableFields.REALM_ID || 
                    modelField == SearchableFields.CLIENT_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaClientModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == SearchableFields.ENABLED) {
                    validateValue(value, modelField, op, Boolean.class);

                    return new JpaClientModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == SearchableFields.SCOPE_MAPPING_ROLE) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaClientModelCriteriaBuilder((cb, query, root) ->
                        cb.isTrue(cb.function("@>",
                            Boolean.TYPE,
                            cb.function("->", JsonbType.class, root.get("metadata"), cb.literal("fScopeMappings")),
                            cb.literal(convertToJson(value[0]))))
                    );
                } else if (modelField == SearchableFields.ALWAYS_DISPLAY_IN_CONSOLE) {
                    validateValue(value, modelField, op, Boolean.class);

                    return new JpaClientModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(
                            cb.function("->", JsonbType.class, root.get("metadata"), cb.literal("fAlwaysDisplayInConsole")), 
                            cb.literal(convertToJson(value[0])))
                    );
                } else if (modelField == SearchableFields.ATTRIBUTE) {
                    validateValue(value, modelField, op, String.class, String.class);

                    return new JpaClientModelCriteriaBuilder((cb, query, root) -> {
                        Join<JpaClientEntity, JpaClientAttributeEntity> join = root.join("attributes", JoinType.LEFT);
                        return cb.and(
                            cb.equal(join.get("name"), value[0]), 
                            cb.equal(join.get("value"), value[1])
                        );
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case ILIKE:
                if (modelField == SearchableFields.CLIENT_ID) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaClientModelCriteriaBuilder((cb, query, root) ->
                        cb.like(cb.lower(root.get(modelField.getName())), value[0].toString().toLowerCase())
                    );
                }
            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }
}
