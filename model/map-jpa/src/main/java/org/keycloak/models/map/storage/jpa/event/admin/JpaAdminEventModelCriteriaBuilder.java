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
package org.keycloak.models.map.storage.jpa.event.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.event.admin.entity.JpaAdminEventEntity;
import org.keycloak.models.map.storage.jpa.role.JpaPredicateFunction;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.util.EnumWithStableIndex;

/**
 * A {@link JpaModelCriteriaBuilder} implementation for admin events.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaAdminEventModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaAdminEventEntity, AdminEvent, JpaAdminEventModelCriteriaBuilder> {

    private static final Map<String, String> FIELD_TO_JSON_PROP = new HashMap<>();
    static {
        FIELD_TO_JSON_PROP.put(AdminEvent.SearchableFields.AUTH_CLIENT_ID.getName(), "fAuthClientId");
        FIELD_TO_JSON_PROP.put(AdminEvent.SearchableFields.AUTH_REALM_ID.getName(), "fAuthRealmId");
        FIELD_TO_JSON_PROP.put(AdminEvent.SearchableFields.AUTH_USER_ID.getName(), "fAuthUserId");
        FIELD_TO_JSON_PROP.put(AdminEvent.SearchableFields.AUTH_IP_ADDRESS.getName(), "fAuthIpAddress");
        FIELD_TO_JSON_PROP.put(AdminEvent.SearchableFields.RESOURCE_PATH.getName(), "fResourcePath");
        FIELD_TO_JSON_PROP.put(AdminEvent.SearchableFields.RESOURCE_TYPE.getName(), "fResourceType");
        FIELD_TO_JSON_PROP.put(AdminEvent.SearchableFields.OPERATION_TYPE.getName(), "fOperationType");
    }

    public JpaAdminEventModelCriteriaBuilder() {
        super(JpaAdminEventModelCriteriaBuilder::new);
    }

    private JpaAdminEventModelCriteriaBuilder(JpaPredicateFunction<JpaAdminEventEntity> predicateFunc) {
        super(JpaAdminEventModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaAdminEventModelCriteriaBuilder compare(SearchableModelField<? super AdminEvent> modelField, Operator op, Object... value) {
        switch(op) {
            case EQ:
                if (modelField == AdminEvent.SearchableFields.REALM_ID) {

                    validateValue(value, modelField, op, String.class);
                    return new JpaAdminEventModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == AdminEvent.SearchableFields.AUTH_CLIENT_ID ||
                        modelField == AdminEvent.SearchableFields.AUTH_REALM_ID ||
                        modelField == AdminEvent.SearchableFields.AUTH_USER_ID ||
                        modelField == AdminEvent.SearchableFields.AUTH_IP_ADDRESS) {

                    validateValue(value, modelField, op, String.class);
                    return new JpaAdminEventModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(
                                    cb.function("->>", String.class, root.get("metadata"),
                                            cb.literal(FIELD_TO_JSON_PROP.get(modelField.getName()))), value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case LE:
                if (modelField == AdminEvent.SearchableFields.TIMESTAMP) {

                    validateValue(value, modelField, op, Number.class);

                    return new JpaAdminEventModelCriteriaBuilder((cb, query, root) ->
                            cb.le(root.get(modelField.getName()), (Number) value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case LT:
                if (modelField == AdminEvent.SearchableFields.TIMESTAMP) {
                    validateValue(value, modelField, op, Number.class);

                    return new JpaAdminEventModelCriteriaBuilder((cb, query, root) ->
                            cb.lt(root.get(modelField.getName()), (Number) value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case LIKE:
                if (modelField == AdminEvent.SearchableFields.RESOURCE_PATH) {
                    validateValue(value, modelField, op, String.class);

                    return new JpaAdminEventModelCriteriaBuilder((cb, query, root) ->
                            cb.like(
                                    cb.function("->>", String.class, root.get("metadata"), cb.literal(FIELD_TO_JSON_PROP.get(modelField.getName()))),
                                    value[0].toString())
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case GE:
                if (modelField == AdminEvent.SearchableFields.TIMESTAMP) {
                    validateValue(value, modelField, op, Number.class);

                    return new JpaAdminEventModelCriteriaBuilder((cb, query, root) ->
                            cb.ge(root.get(modelField.getName()), (Number) value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case IN:
                if (modelField == AdminEvent.SearchableFields.OPERATION_TYPE) {
                    Set<Integer> values = super.getValuesForInOperator(value, modelField).stream()
                            .map(o -> ((EnumWithStableIndex) o).getStableIndex()).collect(Collectors.toSet());

                    if (values.isEmpty()) return new JpaAdminEventModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaAdminEventModelCriteriaBuilder((cb, query, root) -> {
                        CriteriaBuilder.In<Integer> in = cb.in(cb.function("->>", String.class, root.get("metadata"),
                                cb.literal(FIELD_TO_JSON_PROP.get(modelField.getName()))).as(Integer.class));
                        values.forEach(in::value);
                        return in;
                    });
                }
                else if (modelField == AdminEvent.SearchableFields.RESOURCE_TYPE) {
                    Set<String> values = super.getValuesForInOperator(value, modelField).stream()
                            .map(Object::toString).collect(Collectors.toSet());

                    if (values.isEmpty()) return new JpaAdminEventModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaAdminEventModelCriteriaBuilder((cb, query, root) -> {
                        CriteriaBuilder.In<String> in = cb.in(cb.function("->>", String.class, root.get("metadata"),
                                                              cb.literal(FIELD_TO_JSON_PROP.get(modelField.getName()))));
                        values.forEach(in::value);
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
