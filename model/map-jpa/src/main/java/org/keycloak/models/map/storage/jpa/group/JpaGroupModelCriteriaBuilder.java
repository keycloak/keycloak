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
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.keycloak.models.GroupModel;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.group.entity.JpaGroupEntity;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.storage.SearchableModelField;

public class JpaGroupModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaGroupEntity, GroupModel, JpaGroupModelCriteriaBuilder> {

    public JpaGroupModelCriteriaBuilder() {
        super(JpaGroupModelCriteriaBuilder::new);
    }

    private JpaGroupModelCriteriaBuilder(BiFunction<CriteriaBuilder, Root<JpaGroupEntity>, Predicate> predicateFunc) {
        super(JpaGroupModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JpaGroupModelCriteriaBuilder compare(SearchableModelField<? super GroupModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField.equals(GroupModel.SearchableFields.REALM_ID) ||
                    modelField.equals(GroupModel.SearchableFields.NAME)) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaGroupModelCriteriaBuilder((cb, root) -> 
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField.equals(GroupModel.SearchableFields.PARENT_ID)) {
                    if (value.length == 1 && Objects.isNull(value[0])) {
                        return new JpaGroupModelCriteriaBuilder((cb, root) -> 
                            cb.isNull(root.get("parentId"))
                        );
                    }

                    validateValue(value, modelField, op, String.class);

                    return new JpaGroupModelCriteriaBuilder((cb, root) -> 
                        cb.equal(root.get("parentId"), value[0])
                    );
                } else if (modelField.equals(GroupModel.SearchableFields.ASSIGNED_ROLE)) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaGroupModelCriteriaBuilder((cb, root) -> 
                        cb.isTrue(cb.function("@>",
                            Boolean.TYPE,
                            cb.function("->", JsonbType.class, root.get("metadata"), cb.literal("fGrantedRoles")),
                            cb.literal(convertToJson(value[0]))))
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case IN:
                if (modelField.equals(GroupModel.SearchableFields.ID)) {
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

                    if (collectionValues.isEmpty()) {
                        return new JpaGroupModelCriteriaBuilder((cb, root) -> cb.or());
                    }

                    return new JpaGroupModelCriteriaBuilder((cb, root) ->  {
                        CriteriaBuilder.In<UUID> in = cb.in(root.get("id"));
                        for (Object id : collectionValues) {
                            try {
                                in.value(StringKeyConverter.UUIDKey.INSTANCE.fromString(Objects.toString(id, null)));
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
                if (modelField.equals(GroupModel.SearchableFields.NAME)) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaGroupModelCriteriaBuilder((cb, root) -> 
                        cb.like(cb.lower(root.get(modelField.getName())), value[0].toString().toLowerCase())
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case NOT_EXISTS:
                if (modelField.equals(GroupModel.SearchableFields.PARENT_ID)) {

                    return new JpaGroupModelCriteriaBuilder((cb, root) -> 
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
