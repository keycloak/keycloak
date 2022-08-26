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
package org.keycloak.models.map.storage.jpa.group;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;

import org.keycloak.models.GroupModel;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.group.entity.JpaGroupEntity;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaPredicateFunction;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.storage.SearchableModelField;

public class JpaGroupModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaGroupEntity, GroupModel, JpaGroupModelCriteriaBuilder> {

    public JpaGroupModelCriteriaBuilder() {
        super(JpaGroupModelCriteriaBuilder::new);
    }

    private JpaGroupModelCriteriaBuilder(JpaPredicateFunction<JpaGroupEntity> predicateFunc) {
        super(JpaGroupModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaGroupModelCriteriaBuilder compare(SearchableModelField<? super GroupModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == GroupModel.SearchableFields.REALM_ID ||
                    modelField == GroupModel.SearchableFields.NAME) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaGroupModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == GroupModel.SearchableFields.PARENT_ID) {
                    if (value.length == 1 && Objects.isNull(value[0])) {
                        return new JpaGroupModelCriteriaBuilder((cb, query, root) ->
                            cb.isNull(root.get("parentId"))
                        );
                    }

                    validateValue(value, modelField, op, String.class);

                    return new JpaGroupModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get("parentId"), value[0])
                    );
                } else if (modelField == GroupModel.SearchableFields.ASSIGNED_ROLE) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaGroupModelCriteriaBuilder((cb, query, root) ->
                        cb.isTrue(cb.function("@>",
                            Boolean.TYPE,
                            cb.function("->", JsonbType.class, root.get("metadata"), cb.literal("fGrantedRoles")),
                            cb.literal(convertToJson(value[0]))))
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case IN:
                if (modelField == GroupModel.SearchableFields.ID) {

                    Set<UUID> uuids = getUuidsForInOperator(value, modelField);

                    if (uuids.isEmpty()) return new JpaGroupModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaGroupModelCriteriaBuilder((cb, query, root) ->  {
                        CriteriaBuilder.In<UUID> in = cb.in(root.get("id"));
                        uuids.forEach(uuid -> in.value(uuid));
                        return in;
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case ILIKE:
                if (modelField == GroupModel.SearchableFields.NAME) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaGroupModelCriteriaBuilder((cb, query, root) ->
                        cb.like(cb.lower(root.get(modelField.getName())), value[0].toString().toLowerCase())
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case NOT_EXISTS:
                if (modelField == GroupModel.SearchableFields.PARENT_ID) {

                    return new JpaGroupModelCriteriaBuilder((cb, query, root) ->
                        cb.isNull(root.get("parentId"))
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }
}
