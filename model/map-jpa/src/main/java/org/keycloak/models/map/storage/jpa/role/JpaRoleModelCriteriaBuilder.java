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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleModel.SearchableFields;
import org.keycloak.models.map.common.StringKeyConvertor.UUIDKey;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleEntity;
import org.keycloak.storage.SearchableModelField;

public class JpaRoleModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaRoleEntity, RoleModel, JpaRoleModelCriteriaBuilder> {

    public JpaRoleModelCriteriaBuilder() {
        super(JpaRoleModelCriteriaBuilder::new);
    }

    private JpaRoleModelCriteriaBuilder(BiFunction<CriteriaBuilder, Root<JpaRoleEntity>, Predicate> predicateFunc) {
        super(JpaRoleModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JpaRoleModelCriteriaBuilder compare(SearchableModelField<? super RoleModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField.equals(SearchableFields.REALM_ID) ||
                    modelField.equals(SearchableFields.CLIENT_ID) ||
                    modelField.equals(SearchableFields.NAME)) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaRoleModelCriteriaBuilder((cb, root) -> 
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case NE:
                if (modelField.equals(SearchableFields.IS_CLIENT_ROLE)) {
                    
                    validateValue(value, modelField, op, Boolean.class);

                    return new JpaRoleModelCriteriaBuilder((cb, root) -> 
                        ((Boolean) value[0]) ? cb.isNull(root.get("clientId")) : cb.isNotNull(root.get("clientId"))
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case IN:
                if (modelField.equals(SearchableFields.ID)) {
                    if (value == null || value.length == 0) throw new CriterionNotSupportedException(modelField, op);

                    final Collection<?> collectionValues;
                    if (value.length == 1) {

                        if (value[0] instanceof Object[]) {
                            collectionValues = Arrays.asList(value[0]);
                        } else if (value[0] instanceof Collection) {
                            collectionValues = (Collection) value[0];
                        } else if (value[0] instanceof Stream) {
                            try (Stream<?> str = ((Stream) value[0])) {
                                collectionValues = str.collect(Collectors.toCollection(ArrayList::new));
                            }
                        } else {
                            collectionValues = Collections.singleton(value[0]);
                        }

                    } else  {
                        collectionValues = new HashSet(Arrays.asList(value));
                    }

                    return new JpaRoleModelCriteriaBuilder((cb, root) ->  {
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
            case ILIKE:
                if (modelField.equals(SearchableFields.NAME) ||
                    modelField.equals(SearchableFields.DESCRIPTION)) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaRoleModelCriteriaBuilder((cb, root) -> 
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
