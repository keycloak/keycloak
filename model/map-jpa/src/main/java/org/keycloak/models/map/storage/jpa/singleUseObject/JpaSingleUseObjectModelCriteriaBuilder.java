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
package org.keycloak.models.map.storage.jpa.singleUseObject;

import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaPredicateFunction;
import org.keycloak.models.map.storage.jpa.singleUseObject.entity.JpaSingleUseObjectEntity;
import org.keycloak.storage.SearchableModelField;

/**
 * A {@link JpaModelCriteriaBuilder} implementation for single-use objects.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaSingleUseObjectModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaSingleUseObjectEntity, SingleUseObjectValueModel, JpaSingleUseObjectModelCriteriaBuilder> {

    public JpaSingleUseObjectModelCriteriaBuilder() {
        super(JpaSingleUseObjectModelCriteriaBuilder::new);
    }

    public JpaSingleUseObjectModelCriteriaBuilder(JpaPredicateFunction<JpaSingleUseObjectEntity> predicateFunc) {
        super(JpaSingleUseObjectModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaSingleUseObjectModelCriteriaBuilder compare(SearchableModelField<? super SingleUseObjectValueModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if(modelField == SingleUseObjectValueModel.SearchableFields.OBJECT_KEY) {
                    validateValue(value, modelField, op, String.class);
                    return new JpaSingleUseObjectModelCriteriaBuilder((cb, query, root) ->
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
