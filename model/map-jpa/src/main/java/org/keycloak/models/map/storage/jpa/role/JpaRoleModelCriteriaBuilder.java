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
package org.keycloak.models.map.storage.jpa.role;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleModel.SearchableFields;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleCompositeEntity;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleEntity;
import org.keycloak.storage.SearchableModelField;

public class JpaRoleModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaRoleEntity, RoleModel, JpaRoleModelCriteriaBuilder> {

    public JpaRoleModelCriteriaBuilder() {
        super(JpaRoleModelCriteriaBuilder::new);
    }

    private JpaRoleModelCriteriaBuilder(JpaPredicateFunction<JpaRoleEntity> predicateFunc) {
        super(JpaRoleModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaRoleModelCriteriaBuilder compare(SearchableModelField<? super RoleModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == SearchableFields.REALM_ID ||
                    modelField == SearchableFields.CLIENT_ID ||
                    modelField == SearchableFields.NAME) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaRoleModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == SearchableFields.COMPOSITE_ROLE) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaRoleModelCriteriaBuilder((cb, query, root) -> {

                        // using a sub-query here instead of a join as a sub-query will work also for deletions.
                        Subquery<UUID> subquery = query.subquery(UUID.class);
                        Root<JpaRoleCompositeEntity> subRoot = subquery.from(JpaRoleCompositeEntity.class);
                        subquery.select(subRoot.get("key").get("roleId"));
                        In<Object> roleChildId = cb.in(subRoot.get("key").get("childRoleId"));
                        Arrays.stream(value).forEach(roleChildId::value);
                        subquery.where(roleChildId);

                        In<UUID> in = cb.in(root.get("id"));
                        in.value(subquery);
                        return in;
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case NE:
                if (modelField == SearchableFields.IS_CLIENT_ROLE) {
                    
                    validateValue(value, modelField, op, Boolean.class);

                    return new JpaRoleModelCriteriaBuilder((cb, query, root) ->
                        ((Boolean) value[0]) ? cb.isNull(root.get("clientId")) : cb.isNotNull(root.get("clientId"))
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case IN:
                if (modelField ==SearchableFields.ID) {

                    Set<UUID> uuids = getUuidsForInOperator(value, modelField);

                    if (uuids.isEmpty()) return new JpaRoleModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaRoleModelCriteriaBuilder((cb, query, root) ->  {
                        In<UUID> in = cb.in(root.get("id"));
                        uuids.forEach(in::value);
                        return in;
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case ILIKE:
                if (modelField == SearchableFields.NAME ||
                    modelField == SearchableFields.DESCRIPTION) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaRoleModelCriteriaBuilder((cb, query, root) ->
                        cb.like(cb.lower(root.get(modelField.getName())), value[0].toString().toLowerCase())
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }
}
