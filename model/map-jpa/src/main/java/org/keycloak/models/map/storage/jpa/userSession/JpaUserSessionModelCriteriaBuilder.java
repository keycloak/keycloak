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
package org.keycloak.models.map.storage.jpa.userSession;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;

import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionModel.SearchableFields;
import org.keycloak.models.map.common.StringKeyConverter.UUIDKey;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaPredicateFunction;
import org.keycloak.models.map.storage.jpa.userSession.entity.JpaUserSessionEntity;
import org.keycloak.models.map.storage.jpa.userSession.entity.JpaUserSessionNoteEntity;
import org.keycloak.storage.SearchableModelField;

public class JpaUserSessionModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaUserSessionEntity, UserSessionModel, JpaUserSessionModelCriteriaBuilder> {

    public JpaUserSessionModelCriteriaBuilder() {
        super(JpaUserSessionModelCriteriaBuilder::new);
    }

    private JpaUserSessionModelCriteriaBuilder(JpaPredicateFunction<JpaUserSessionEntity> predicateFunc) {
        super(JpaUserSessionModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaUserSessionModelCriteriaBuilder compare(SearchableModelField<? super UserSessionModel> modelField, Operator op, Object... value) {
        switch(op) {
            case EQ:
                if (modelField == SearchableFields.ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaUserSessionModelCriteriaBuilder((cb, query, root) -> {
                        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(Objects.toString(value[0], null));
                        if (uuid == null) return cb.or();
                        return cb.equal(root.get(modelField.getName()), uuid);
                    });
                } else if (modelField == SearchableFields.REALM_ID ||
                           modelField == SearchableFields.USER_ID ||
                           modelField == SearchableFields.BROKER_USER_ID ||
                           modelField == SearchableFields.BROKER_SESSION_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaUserSessionModelCriteriaBuilder((cb, query, root) -> 
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else if (modelField == SearchableFields.IS_OFFLINE) {

                    validateValue(value, modelField, op, Boolean.class);

                    return new JpaUserSessionModelCriteriaBuilder((cb, query, root) -> 
                        cb.equal(root.get("offline"), value[0])
                    );
                } else if (modelField == SearchableFields.CORRESPONDING_SESSION_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaUserSessionModelCriteriaBuilder((cb, query, root) ->  {
                        Join<JpaUserSessionEntity, JpaUserSessionNoteEntity> join = root.join("notes", JoinType.LEFT);
                        return cb.and(
                            cb.equal(join.get("name"), UserSessionModel.CORRESPONDING_SESSION_ID), 
                            cb.equal(join.get("value"), value[0])
                        );
                    });
                } else if (modelField == SearchableFields.CLIENT_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaUserSessionModelCriteriaBuilder((cb, query, root) ->  {
                        return cb.equal(root.join("clientSessions", JoinType.LEFT).get("clientId"), value[0]);
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }
}
