/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.criteria.ModelCriteriaNode.ExtOperator;
import org.keycloak.storage.SearchableModelField;

/**
 * Descriptive model criteria implementation which in other words represents a Boolean formula on searchable fields.
 * @author hmlnarik
 */
public class DefaultModelCriteria<M> implements ModelCriteriaBuilder<M> {

    private final ModelCriteriaNode<M> node;

    public DefaultModelCriteria() {
        this.node = null;
    }

    private DefaultModelCriteria(ModelCriteriaNode<M> node) {
        this.node = node;
    }

    @Override
    public DefaultModelCriteria<M> compare(SearchableModelField<M> modelField, Operator op, Object... value) {
        final ModelCriteriaNode<M> targetNode;
        if (isEmpty()) {
            targetNode = new ModelCriteriaNode<>(modelField, op, value);
        } else if (node.getNodeOperator() == ExtOperator.AND) {
            targetNode = node.cloneTree();
            targetNode.addChild(new ModelCriteriaNode<>(modelField, op, value));
        } else {
            targetNode = new ModelCriteriaNode<>(ExtOperator.AND);
            targetNode.addChild(node.cloneTree());
            targetNode.addChild(new ModelCriteriaNode<>(modelField, op, value));
        }
        return new DefaultModelCriteria<>(targetNode);
    }

    @Override
    public DefaultModelCriteria<M> and(ModelCriteriaBuilder<M>... mcbs) {
        final ModelCriteriaNode<M> targetNode = new ModelCriteriaNode<>(ExtOperator.AND);
        for (ModelCriteriaBuilder<M> mcb : mcbs) {
            targetNode.addChild(((DefaultModelCriteria<M>) mcb.unwrap(DefaultModelCriteria.class)).node);
        }
        return new DefaultModelCriteria<>(targetNode);
    }

    @Override
    public DefaultModelCriteria<M> or(ModelCriteriaBuilder<M>... mcbs) {
        final ModelCriteriaNode<M> targetNode = new ModelCriteriaNode<>(ExtOperator.OR);
        for (ModelCriteriaBuilder<M> mcb : mcbs) {
            targetNode.addChild(((DefaultModelCriteria<M>) mcb.unwrap(DefaultModelCriteria.class)).node);
        }
        return new DefaultModelCriteria<>(targetNode);
    }

    @Override
    public DefaultModelCriteria<M> not(ModelCriteriaBuilder<M> mcb) {
        final ModelCriteriaNode<M> targetNode = new ModelCriteriaNode<>(ExtOperator.NOT);
        targetNode.addChild(((DefaultModelCriteria<M>) mcb.unwrap(DefaultModelCriteria.class)).node);
        return new DefaultModelCriteria<>(targetNode);
    }

    /**
     * Copies contents of this {@code ModelCriteriaBuilder} into
     * another {@code ModelCriteriaBuilder}.
     * @param mcb {@code ModelCriteriaBuilder} to copy the contents onto
     * @return Updated {@code ModelCriteriaBuilder}
     */
    public ModelCriteriaBuilder<M> flashToModelCriteriaBuilder(ModelCriteriaBuilder<M> mcb) {
        return mcb == null ? null : node.flashToModelCriteriaBuilder(mcb);
    }

    public boolean isEmpty() {
        return node == null;
    }

    @Override
    public String toString() {
        return isEmpty() ? "" : node.toString();
    }

}
