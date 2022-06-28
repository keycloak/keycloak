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
package org.keycloak.models.map.storage.jpa.authorization.resourceServer;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.ResourceServer.SearchableFields;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.authorization.resourceServer.entity.JpaResourceServerEntity;
import org.keycloak.models.map.storage.jpa.role.JpaPredicateFunction;
import org.keycloak.storage.SearchableModelField;

public class JpaResourceServerModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaResourceServerEntity, ResourceServer, JpaResourceServerModelCriteriaBuilder> {

    public JpaResourceServerModelCriteriaBuilder() {
        super(JpaResourceServerModelCriteriaBuilder::new);
    }

    private JpaResourceServerModelCriteriaBuilder(JpaPredicateFunction<JpaResourceServerEntity> predicateFunc) {
        super(JpaResourceServerModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaResourceServerModelCriteriaBuilder compare(SearchableModelField<? super ResourceServer> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == SearchableFields.REALM_ID ||
                    modelField == SearchableFields.CLIENT_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaResourceServerModelCriteriaBuilder((cb, query, root) ->
                        cb.equal(root.get(modelField.getName()), value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }
}
