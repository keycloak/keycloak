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

package org.keycloak.models.map.storage.chm;

import org.keycloak.models.ActionTokenValueModel;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.storage.SearchableModelField;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class SingleUseObjectModelCriteriaBuilder implements ModelCriteriaBuilder {

    private String userId;

    private String actionId;

    private String actionVerificationNonce;

    private String objectKey;

    public SingleUseObjectModelCriteriaBuilder() {
    }

    public SingleUseObjectModelCriteriaBuilder(String userId, String actionId, String actionVerificationNonce, String objectKey) {
        this.userId = userId;
        this.actionId = actionId;
        this.actionVerificationNonce = actionVerificationNonce;
        this.objectKey = objectKey;
    }

    @Override
    public ModelCriteriaBuilder compare(SearchableModelField modelField, Operator op, Object... value) {
        if (modelField == org.keycloak.models.ActionTokenValueModel.SearchableFields.USER_ID) {
            userId = value[0].toString();
        } else if (modelField == org.keycloak.models.ActionTokenValueModel.SearchableFields.ACTION_ID) {
            actionId = value[0].toString();
        } else if (modelField == org.keycloak.models.ActionTokenValueModel.SearchableFields.ACTION_VERIFICATION_NONCE) {
            actionVerificationNonce = value[0].toString();
        } else if (modelField == ActionTokenValueModel.SearchableFields.OBJECT_KEY) {
            objectKey = value[0].toString();
        }
        return new SingleUseObjectModelCriteriaBuilder(userId, actionId, actionVerificationNonce, objectKey);
    }

    @Override
    public ModelCriteriaBuilder and(ModelCriteriaBuilder[] builders) {
        String userId = null;
        String actionId = null;
        String actionVerificationNonce = null;
        String objectKey = null;

        for (ModelCriteriaBuilder builder: builders) {
            SingleUseObjectModelCriteriaBuilder suoMcb = (SingleUseObjectModelCriteriaBuilder) builder;
            if (suoMcb.userId != null) {
                userId = suoMcb.userId;
            }
            if (suoMcb.actionId != null) {
                actionId = suoMcb.actionId;
            }
            if (suoMcb.actionVerificationNonce != null) {
                actionVerificationNonce = suoMcb.actionVerificationNonce;
            }
            if (suoMcb.objectKey != null) {
                objectKey = suoMcb.objectKey;
            }
        }
        return new SingleUseObjectModelCriteriaBuilder(userId, actionId, actionVerificationNonce, objectKey);
    }

    @Override
    public ModelCriteriaBuilder or(ModelCriteriaBuilder[] builders) {
        throw new IllegalStateException("SingleUseObjectModelCriteriaBuilder does not support OR operation.");
    }

    @Override
    public ModelCriteriaBuilder not(ModelCriteriaBuilder builder) {
        throw new IllegalStateException("SingleUseObjectModelCriteriaBuilder does not support NOT operation.");
    }

    public boolean isValid() {
        return (userId != null && actionId != null && actionVerificationNonce != null) || objectKey != null;
    }

    public String getKey() {
        if (objectKey != null) return objectKey;
        return userId + ":" + actionId + ":" + actionVerificationNonce;
    }
}
