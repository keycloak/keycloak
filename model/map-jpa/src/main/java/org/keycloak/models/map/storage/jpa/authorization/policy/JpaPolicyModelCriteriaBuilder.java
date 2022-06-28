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
package org.keycloak.models.map.storage.jpa.authorization.policy;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Policy.SearchableFields;
import org.keycloak.models.map.common.StringKeyConverter.UUIDKey;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.authorization.policy.entity.JpaPolicyConfigEntity;
import org.keycloak.models.map.storage.jpa.authorization.policy.entity.JpaPolicyEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.models.map.storage.jpa.role.JpaPredicateFunction;
import org.keycloak.storage.SearchableModelField;

public class JpaPolicyModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaPolicyEntity, Policy, JpaPolicyModelCriteriaBuilder> {

    public JpaPolicyModelCriteriaBuilder() {
        super(JpaPolicyModelCriteriaBuilder::new);
    }

    private JpaPolicyModelCriteriaBuilder(JpaPredicateFunction<JpaPolicyEntity> predicateFunc) {
        super(JpaPolicyModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaPolicyModelCriteriaBuilder compare(SearchableModelField<? super Policy> modelField, ModelCriteriaBuilder.Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == SearchableFields.ID ||
                    modelField == SearchableFields.RESOURCE_SERVER_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> {
                        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(Objects.toString(value[0], null));
                        if (uuid == null) return cb.or();
                        return cb.equal(root.get(modelField.getName()), uuid);
                    });
                } else if (modelField == SearchableFields.REALM_ID ||
                           modelField == SearchableFields.NAME ||
                           modelField == SearchableFields.TYPE) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == SearchableFields.ASSOCIATED_POLICY_ID) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> {
                        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(Objects.toString(value[0], null));
                        if (uuid == null) return cb.or();
                        return cb.equal(root.join("policyIds", JoinType.LEFT), uuid);
                    });
                } else if (modelField == SearchableFields.RESOURCE_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> {
                        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(Objects.toString(value[0], null));
                        if (uuid == null) return cb.or();
                        return cb.equal(root.join("resourceIds", JoinType.LEFT), uuid);
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case NOT_EXISTS:
                if (modelField == SearchableFields.OWNER) {
                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) ->
                        cb.isNull(cb.function("->", JsonbType.class, root.get("metadata"), cb.literal("fOwner")))
                    );
                } else if (modelField == SearchableFields.RESOURCE_ID) {
                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> {
                        return cb.isNull(root.join("resourceIds", JoinType.LEFT));
                    });
                } else if (modelField == SearchableFields.CONFIG) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> {
                        Join<JpaPolicyEntity, JpaPolicyConfigEntity> join = root.join("config", JoinType.LEFT);
                        return cb.isNull(join.get("name"));
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case LIKE:
                if (modelField == SearchableFields.CONFIG) {
                    validateValue(value, modelField, op, String.class, String.class);

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) ->  {
                        Join<JpaPolicyEntity, JpaPolicyConfigEntity> join = root.join("config", JoinType.LEFT);
                        return cb.and(
                            cb.equal(join.get("name"), value[0]), 
                            cb.like(join.get("value"), value[1].toString())
                        );
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case ILIKE:
                if (modelField == SearchableFields.NAME ||
                    modelField == SearchableFields.TYPE) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) ->
                        cb.like(cb.lower(root.get(modelField.getName())), value[0].toString().toLowerCase())
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case IN:
                if (modelField == SearchableFields.ID) {

                    Set<UUID> uuids = getUuidsForInOperator(value, modelField);

                    if (uuids.isEmpty()) return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) ->  {
                        In<UUID> in = cb.in(root.get("id"));
                        uuids.forEach(in::value);
                        return in;
                    });
                } else if (modelField == SearchableFields.TYPE) {
                    
                    final Collection<?> collectionValues = getValuesForInOperator(value, modelField);

                    if (collectionValues.isEmpty()) {
                        return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> cb.or());
                    }

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) ->  {
                        In<String> in = cb.in(root.get("type"));
                        for (Object type : collectionValues) {
                            if (! (type instanceof String)) throw new CriterionNotSupportedException(modelField, op, type + " type is not String.");
                            in.value(type.toString());
                        }
                        return in;
                    });
                } else if (modelField == SearchableFields.OWNER) {

                    final Collection<?> collectionValues = getValuesForInOperator(value, modelField);

                    if (collectionValues.isEmpty()) return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) ->  {
                        In<String> in = cb.in(root.get("owner"));
                        collectionValues.forEach(owner -> {
                            if (! (owner instanceof String)) throw new CriterionNotSupportedException(modelField, op, owner + " owner is not String.");
                            in.value(owner.toString());
                        });
                        return in;
                    });
                } else if (modelField == SearchableFields.SCOPE_ID) {

                    Set<UUID> scopeUuids = getUuidsForInOperator(value, modelField);

                    if (scopeUuids.isEmpty()) return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) ->  {
                        In<UUID> in = cb.in(root.join("scopeIds", JoinType.LEFT));
                        scopeUuids.forEach(in::value);
                        return in;
                    });
                } else if (modelField == SearchableFields.RESOURCE_ID) {
                    
                    Set<UUID> resourceUuids = getUuidsForInOperator(value, modelField);

                    if (resourceUuids.isEmpty()) return new JpaPolicyModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaPolicyModelCriteriaBuilder((cb, query, root) ->  {
                        In<UUID> in = cb.in(root.join("resourceIds", JoinType.LEFT));
                        resourceUuids.forEach(in::value);
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
