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

import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.storage.SearchableModelField;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class SingleUseObjectModelCriteriaBuilder implements ModelCriteriaBuilder {

    private String objectKey;

    public SingleUseObjectModelCriteriaBuilder() {
    }

    public SingleUseObjectModelCriteriaBuilder(String objectKey) {
        this.objectKey = objectKey;
    }

    @Override
    public ModelCriteriaBuilder compare(SearchableModelField modelField, Operator op, Object... value) {
        if (modelField == SingleUseObjectValueModel.SearchableFields.OBJECT_KEY) {
            objectKey = value[0].toString();
        }
        return new SingleUseObjectModelCriteriaBuilder(objectKey);
    }

    @Override
    public ModelCriteriaBuilder and(ModelCriteriaBuilder[] builders) {
        String objectKey = null;

        for (ModelCriteriaBuilder builder: builders) {
            SingleUseObjectModelCriteriaBuilder suoMcb = (SingleUseObjectModelCriteriaBuilder) builder;
            if (suoMcb.objectKey != null) {
                objectKey = suoMcb.objectKey;
            }
        }
        return new SingleUseObjectModelCriteriaBuilder(objectKey);
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
        return objectKey != null;
    }

    public String getKey() {
        return objectKey;
    }
}
