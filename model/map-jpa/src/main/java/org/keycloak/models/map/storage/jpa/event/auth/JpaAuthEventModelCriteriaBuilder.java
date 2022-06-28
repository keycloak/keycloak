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
package org.keycloak.models.map.storage.jpa.event.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;

import org.keycloak.events.Event;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.event.auth.entity.JpaAuthEventEntity;
import org.keycloak.models.map.storage.jpa.role.JpaPredicateFunction;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.util.EnumWithStableIndex;

public class JpaAuthEventModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaAuthEventEntity, Event, JpaAuthEventModelCriteriaBuilder> {

    private static final Map<String, String> FIELD_TO_JSON_PROP = new HashMap<>();
    static {
        FIELD_TO_JSON_PROP.put(Event.SearchableFields.CLIENT_ID.getName(), "fClientId");
        FIELD_TO_JSON_PROP.put(Event.SearchableFields.USER_ID.getName(), "fUserId");
        FIELD_TO_JSON_PROP.put(Event.SearchableFields.IP_ADDRESS.getName(), "fIpAddress");
        FIELD_TO_JSON_PROP.put(Event.SearchableFields.EVENT_TYPE.getName(), "fType");
    }

    public JpaAuthEventModelCriteriaBuilder() {
        super(JpaAuthEventModelCriteriaBuilder::new);
    }

    private JpaAuthEventModelCriteriaBuilder(JpaPredicateFunction<JpaAuthEventEntity> predicateFunc) {
        super(JpaAuthEventModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaAuthEventModelCriteriaBuilder compare(SearchableModelField<? super Event> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == Event.SearchableFields.REALM_ID) {

                    validateValue(value, modelField, op, String.class);
                    return new JpaAuthEventModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == Event.SearchableFields.CLIENT_ID ||
                        modelField == Event.SearchableFields.USER_ID ||
                        modelField == Event.SearchableFields.IP_ADDRESS) {

                    validateValue(value, modelField, op, String.class);
                    return new JpaAuthEventModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(
                                    cb.function("->>", String.class, root.get("metadata"),
                                            cb.literal(FIELD_TO_JSON_PROP.get(modelField.getName()))), value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case LE:
                if (modelField == Event.SearchableFields.TIMESTAMP) {

                    validateValue(value, modelField, op, Number.class);

                    return new JpaAuthEventModelCriteriaBuilder((cb, query, root) ->
                            cb.le(root.get(modelField.getName()), (Number) value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case LT:
                if (modelField == Event.SearchableFields.TIMESTAMP) {
                    validateValue(value, modelField, op, Number.class);

                    return new JpaAuthEventModelCriteriaBuilder((cb, query, root) ->
                            cb.lt(root.get(modelField.getName()), (Number) value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case GE:
                if (modelField == Event.SearchableFields.TIMESTAMP) {
                    validateValue(value, modelField, op, Number.class);

                    return new JpaAuthEventModelCriteriaBuilder((cb, query, root) ->
                            cb.ge(root.get(modelField.getName()), (Number) value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case IN:
                if (modelField == Event.SearchableFields.EVENT_TYPE) {
                    Set<Integer> values = super.getValuesForInOperator(value, modelField).stream()
                            .map(o -> ((EnumWithStableIndex) o).getStableIndex()).collect(Collectors.toSet());

                    if (values.isEmpty()) return new JpaAuthEventModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaAuthEventModelCriteriaBuilder((cb, query, root) -> {
                        CriteriaBuilder.In<Integer> in = cb.in(cb.function("->>", String.class, root.get("metadata"),
                                cb.literal(FIELD_TO_JSON_PROP.get(modelField.getName()))).as(Integer.class));
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
