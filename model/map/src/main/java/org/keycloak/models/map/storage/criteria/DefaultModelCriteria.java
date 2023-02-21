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
package org.keycloak.models.map.storage.criteria;

/**
 * Generic instantiable {@link DescriptiveModelCriteria}.
 * @author hmlnarik
 */
public class DefaultModelCriteria<M> extends DescriptiveModelCriteria<M, DefaultModelCriteria<M>> {

    private static final DefaultModelCriteria<?> INSTANCE = new DefaultModelCriteria<>(null);

    private DefaultModelCriteria(ModelCriteriaNode<M> node) {
        super(node);
    }

    @SuppressWarnings("unchecked")
    public static <M> DefaultModelCriteria<M> criteria() {
        return (DefaultModelCriteria<M>) INSTANCE;
    }

    @Override
    protected DefaultModelCriteria<M> instantiateForNode(ModelCriteriaNode<M> targetNode) {
        return new DefaultModelCriteria<>(targetNode);
    }

}
