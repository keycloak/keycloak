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
package org.keycloak.models.map.storage.jpa.authorization.resource;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.JoinType;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Resource.SearchableFields;
import org.keycloak.models.map.common.StringKeyConverter.UUIDKey;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.authorization.resource.entity.JpaResourceEntity;
import org.keycloak.models.map.storage.jpa.role.JpaPredicateFunction;
import org.keycloak.storage.SearchableModelField;

public class JpaResourceModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaResourceEntity, Resource, JpaResourceModelCriteriaBuilder> {

    public JpaResourceModelCriteriaBuilder() {
        super(JpaResourceModelCriteriaBuilder::new);
    }

    private JpaResourceModelCriteriaBuilder(JpaPredicateFunction<JpaResourceEntity> predicateFunc) {
        super(JpaResourceModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaResourceModelCriteriaBuilder compare(SearchableModelField<? super Resource> modelField, ModelCriteriaBuilder.Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == SearchableFields.ID ||
                    modelField == SearchableFields.RESOURCE_SERVER_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) -> {
                        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(Objects.toString(value[0], null));
                        if (uuid == null) return cb.or();
                        return cb.equal(root.get(modelField.getName()), uuid);
                    });
                } else if (modelField == SearchableFields.REALM_ID ||
                           modelField == SearchableFields.NAME ||
                           modelField == SearchableFields.TYPE ||
                           modelField == SearchableFields.OWNER) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == SearchableFields.OWNER_MANAGED_ACCESS) {
                    validateValue(value, modelField, op, Boolean.class);

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == SearchableFields.URI) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) -> {
                        return cb.equal(root.join("uris", JoinType.LEFT), value[0]);
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case NE:
                if (modelField == SearchableFields.OWNER) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0]).not()
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case EXISTS:
                if (modelField == SearchableFields.URI) {
                    return new JpaResourceModelCriteriaBuilder((cb, query, root) -> {
                        return cb.isNotNull(root.join("uris", JoinType.LEFT));
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case ILIKE:
                if (modelField == SearchableFields.NAME ||
                    modelField == SearchableFields.TYPE) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) ->
                        cb.like(cb.lower(root.get(modelField.getName())), value[0].toString().toLowerCase())
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case IN:
                if (modelField == SearchableFields.ID) {

                    Set<UUID> uuids = getUuidsForInOperator(value, modelField);

                    if (uuids.isEmpty()) return new JpaResourceModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) ->  {
                        In<UUID> in = cb.in(root.get("id"));
                        uuids.forEach(uuid -> in.value(uuid));
                        return in;
                    });
                } else if (modelField == SearchableFields.OWNER) {

                    final Collection<?> collectionValues = getValuesForInOperator(value, modelField);

                    if (collectionValues.isEmpty()) return new JpaResourceModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) ->  {
                        In<String> in = cb.in(root.get("owner"));
                        collectionValues.forEach(owner -> {
                            if (! (owner instanceof String)) throw new CriterionNotSupportedException(modelField, op, owner + " owner is not String.");
                            in.value(owner.toString());
                        });
                        return in;
                    });
                } else if (modelField == SearchableFields.SCOPE_ID) {

                    Set<UUID> scopeUuids = getUuidsForInOperator(value, modelField);

                    if (scopeUuids.isEmpty()) return new JpaResourceModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) ->  {
                        In<UUID> in = cb.in(root.join("scopeIds", JoinType.LEFT));
                        scopeUuids.forEach(in::value);
                        return in;
                    });
                } else if (modelField == SearchableFields.URI) {

                    final Collection<?> collectionValues = getValuesForInOperator(value, modelField);

                    if (collectionValues.isEmpty()) return new JpaResourceModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaResourceModelCriteriaBuilder((cb, query, root) ->  {
                        In<String> in = cb.in(root.join("uris", JoinType.LEFT));
                        collectionValues.forEach(uri -> {
                            if (! (uri instanceof String)) throw new CriterionNotSupportedException(modelField, op, uri + " uri is not String.");
                            in.value(uri.toString());
                        });
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
