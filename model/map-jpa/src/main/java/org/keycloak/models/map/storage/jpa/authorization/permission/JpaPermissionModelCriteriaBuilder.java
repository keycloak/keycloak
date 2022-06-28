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
package org.keycloak.models.map.storage.jpa.authorization.permission;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder.In;

import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.PermissionTicket.SearchableFields;
import org.keycloak.models.map.common.StringKeyConverter.UUIDKey;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.authorization.permission.entity.JpaPermissionEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.models.map.storage.jpa.role.JpaPredicateFunction;
import org.keycloak.storage.SearchableModelField;

public class JpaPermissionModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaPermissionEntity, PermissionTicket, JpaPermissionModelCriteriaBuilder> {

    public JpaPermissionModelCriteriaBuilder() {
        super(JpaPermissionModelCriteriaBuilder::new);
    }

    private JpaPermissionModelCriteriaBuilder(JpaPredicateFunction<JpaPermissionEntity> predicateFunc) {
        super(JpaPermissionModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaPermissionModelCriteriaBuilder compare(SearchableModelField<? super PermissionTicket> modelField, ModelCriteriaBuilder.Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == SearchableFields.ID ||
                    modelField == SearchableFields.SCOPE_ID ||
                    modelField == SearchableFields.POLICY_ID ||
                    modelField == SearchableFields.RESOURCE_ID ||
                    modelField == SearchableFields.RESOURCE_SERVER_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaPermissionModelCriteriaBuilder((cb, query, root) -> {
                        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(Objects.toString(value[0], null));
                        if (uuid == null) return cb.or();
                        return cb.equal(root.get(modelField.getName()), uuid);
                    });
                } else if (modelField ==  SearchableFields.REALM_ID ||
                           modelField ==  SearchableFields.OWNER ||
                           modelField ==  SearchableFields.REQUESTER) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaPermissionModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case EXISTS:
                if (modelField == SearchableFields.SCOPE_ID ||
                    modelField == SearchableFields.REQUESTER) {

                    return new JpaPermissionModelCriteriaBuilder((cb, query, root) ->
                        cb.isNotNull(root.get(modelField.getName()))
                    );
                } else if (modelField ==  SearchableFields.GRANTED_TIMESTAMP) { 

                    return new JpaPermissionModelCriteriaBuilder((cb, query, root) ->
                        cb.isNotNull(cb.function("->", JsonbType.class, root.get("metadata"), cb.literal("fGrantedTimestamp")))
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case NOT_EXISTS:
                if (modelField == SearchableFields.SCOPE_ID ||
                    modelField == SearchableFields.REQUESTER) {

                    return new JpaPermissionModelCriteriaBuilder((cb, query, root) ->
                        cb.isNull(root.get(modelField.getName()))
                    );
                } else if (modelField == SearchableFields.GRANTED_TIMESTAMP) { 

                    return new JpaPermissionModelCriteriaBuilder((cb, query, root) ->
                        cb.isNull(cb.function("->", JsonbType.class, root.get("metadata"), cb.literal("fGrantedTimestamp")))
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case IN:
                if (modelField == SearchableFields.RESOURCE_ID) {

                    Set<UUID> uuids = getUuidsForInOperator(value, modelField);

                    if (uuids.isEmpty()) return new JpaPermissionModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaPermissionModelCriteriaBuilder((cb, query, root) ->  {
                        In<UUID> in = cb.in(root.get(modelField.getName()));
                        uuids.forEach(in::value);
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
