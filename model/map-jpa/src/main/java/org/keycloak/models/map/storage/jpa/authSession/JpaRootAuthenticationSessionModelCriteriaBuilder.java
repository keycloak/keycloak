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
package org.keycloak.models.map.storage.jpa.authSession;

import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaRootAuthenticationSessionEntity;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel.SearchableFields;
import org.keycloak.storage.SearchableModelField;

public class JpaRootAuthenticationSessionModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaRootAuthenticationSessionEntity, RootAuthenticationSessionModel, JpaRootAuthenticationSessionModelCriteriaBuilder> {

    public JpaRootAuthenticationSessionModelCriteriaBuilder() {
        super(JpaRootAuthenticationSessionModelCriteriaBuilder::new);
    }

    private JpaRootAuthenticationSessionModelCriteriaBuilder(BiFunction<CriteriaBuilder, Root<JpaRootAuthenticationSessionEntity>, Predicate> predicateFunc) {
        super(JpaRootAuthenticationSessionModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaRootAuthenticationSessionModelCriteriaBuilder compare(SearchableModelField<? super RootAuthenticationSessionModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == SearchableFields.REALM_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaRootAuthenticationSessionModelCriteriaBuilder((cb, root) -> 
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case LT:
                if (modelField == SearchableFields.EXPIRATION) {
                    validateValue(value, modelField, op, Number.class);

                    Number expiration = (Number) value[0];
                    return new JpaRootAuthenticationSessionModelCriteriaBuilder((cb, root) -> 
                        cb.lt(root.get(modelField.getName()), expiration)
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }
}
