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

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.ActionTokenValueModel;
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
public class JpaSingleUseObjectModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaSingleUseObjectEntity, ActionTokenValueModel, JpaSingleUseObjectModelCriteriaBuilder> {

    private static final Map<String, String> FIELD_TO_JSON_PROP = new HashMap<>();
    static {
        FIELD_TO_JSON_PROP.put(ActionTokenValueModel.SearchableFields.USER_ID.getName(), "fUserId");
        FIELD_TO_JSON_PROP.put(ActionTokenValueModel.SearchableFields.ACTION_ID.getName(), "fActionId");
        FIELD_TO_JSON_PROP.put(ActionTokenValueModel.SearchableFields.ACTION_VERIFICATION_NONCE.getName(), "fActionVerificationNonce");
    }

    public JpaSingleUseObjectModelCriteriaBuilder() {
        super(JpaSingleUseObjectModelCriteriaBuilder::new);
    }

    public JpaSingleUseObjectModelCriteriaBuilder(JpaPredicateFunction<JpaSingleUseObjectEntity> predicateFunc) {
        super(JpaSingleUseObjectModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaSingleUseObjectModelCriteriaBuilder compare(SearchableModelField<? super ActionTokenValueModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == ActionTokenValueModel.SearchableFields.USER_ID ||
                        modelField == ActionTokenValueModel.SearchableFields.ACTION_ID ||
                        modelField == ActionTokenValueModel.SearchableFields.ACTION_VERIFICATION_NONCE) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaSingleUseObjectModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(cb.function("->>", String.class, root.get("metadata"),
                                    cb.literal(FIELD_TO_JSON_PROP.get(modelField.getName()))), value[0])
                    );
                } else if(modelField == ActionTokenValueModel.SearchableFields.OBJECT_KEY) {
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
