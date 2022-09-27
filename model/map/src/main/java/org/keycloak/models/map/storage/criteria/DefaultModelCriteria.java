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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Descriptive model criteria implementation which in other words represents a Boolean formula on searchable fields.
 * @author hmlnarik
 */
public class DefaultModelCriteria<M> implements ModelCriteriaBuilder<M, DefaultModelCriteria<M>> {

    private static final DefaultModelCriteria<?> INSTANCE = new DefaultModelCriteria<>(null);

    private final ModelCriteriaNode<M> node;

    private DefaultModelCriteria(ModelCriteriaNode<M> node) {
        this.node = node;
    }

    public static <M> DefaultModelCriteria<M> criteria() {
        return (DefaultModelCriteria<M>) INSTANCE;
    }

    @Override
    public DefaultModelCriteria<M> compare(SearchableModelField<? super M> modelField, Operator op, Object... value) {
        return compare(new ModelCriteriaNode<>(modelField, op, value));
    }

    private DefaultModelCriteria<M> compare(final ModelCriteriaNode<M> nodeToAdd) {
        ModelCriteriaNode<M> targetNode;

        if (isEmpty()) {
            targetNode = nodeToAdd;
        } else if (node.getNodeOperator() == ExtOperator.AND) {
            targetNode = node.cloneTree();
            targetNode.addChild(nodeToAdd);
        } else {
            targetNode = new ModelCriteriaNode<>(ExtOperator.AND);
            targetNode.addChild(node.cloneTree());
            targetNode.addChild(nodeToAdd);
        }

        return new DefaultModelCriteria<>(targetNode);
    }

    @Override
    public DefaultModelCriteria<M> and(DefaultModelCriteria<M>... mcbs) {
        if (mcbs.length == 1) {
            return compare(mcbs[0].node);
        }

        final ModelCriteriaNode<M> targetNode = new ModelCriteriaNode<>(ExtOperator.AND);
        AtomicBoolean hasFalseNode = new AtomicBoolean(false);
        for (DefaultModelCriteria<M> mcb : mcbs) {
            final ModelCriteriaNode<M> nodeToAdd = mcb.node;
            getNodesToAddForAndOr(nodeToAdd, ExtOperator.AND)
              .filter(ModelCriteriaNode::isNotTrueNode)
              .peek(n -> { if (n.isFalseNode()) hasFalseNode.lazySet(true); })
              .map(ModelCriteriaNode::cloneTree)
              .forEach(targetNode::addChild);

            if (hasFalseNode.get()) {
                return compare(new ModelCriteriaNode<>(ExtOperator.__FALSE__));
            }
        }

        if (targetNode.getChildren().isEmpty()) {
            // AND on empty set of formulae is TRUE: It does hold that there all formulae are satisfied
            return compare(new ModelCriteriaNode<>(ExtOperator.__TRUE__));
        }

        return compare(targetNode);
    }

    @Override
    public DefaultModelCriteria<M> or(DefaultModelCriteria<M>... mcbs) {
        if (mcbs.length == 1) {
            return compare(mcbs[0].node);
        }

        final ModelCriteriaNode<M> targetNode = new ModelCriteriaNode<>(ExtOperator.OR);
        AtomicBoolean hasTrueNode = new AtomicBoolean(false);
        for (DefaultModelCriteria<M> mcb : mcbs) {
            final ModelCriteriaNode<M> nodeToAdd = mcb.node;
            getNodesToAddForAndOr(nodeToAdd, ExtOperator.OR)
              .filter(ModelCriteriaNode::isNotFalseNode)
              .peek(n -> { if (n.isTrueNode()) hasTrueNode.lazySet(true); })
              .map(ModelCriteriaNode::cloneTree)
              .forEach(targetNode::addChild);

            if (hasTrueNode.get()) {
                return compare(new ModelCriteriaNode<>(ExtOperator.__TRUE__));
            }
        }

        if (targetNode.getChildren().isEmpty()) {
            // OR on empty set of formulae is FALSE: It does not hold that there is at least one satisfied formula
            return compare(new ModelCriteriaNode<>(ExtOperator.__FALSE__));
        }

        return compare(targetNode);
    }

    @Override
    public DefaultModelCriteria<M> not(DefaultModelCriteria<M> mcb) {
        ModelCriteriaNode<M> toBeChild = mcb.node;
        if (toBeChild.getNodeOperator() == ExtOperator.NOT) {
            return compare(toBeChild.getChildren().get(0).cloneTree());
        }

        final ModelCriteriaNode<M> targetNode = new ModelCriteriaNode<>(ExtOperator.NOT);
        targetNode.addChild(toBeChild.cloneTree());
        return compare(targetNode);
    }

    /**
     * Copies contents of this {@code ModelCriteriaBuilder} into
     * another {@code ModelCriteriaBuilder}.
     * @param mcb {@code ModelCriteriaBuilder} to copy the contents onto
     * @return Updated {@code ModelCriteriaBuilder}
     */
    public <C extends ModelCriteriaBuilder<M, C>> C flashToModelCriteriaBuilder(C mcb) {
        if (isEmpty()) {
            return mcb;
        }
        return mcb == null ? null : node.flashToModelCriteriaBuilder(mcb);
    }

    /**
     * Optimizes this formula into another {@code ModelCriteriaBuilder}, using the values of
     * {@link ExtOperator#__TRUE__} and {@link ExtOperator#__FALSE__} accordingly.
     * @return New instance of {@code }
     */
    public DefaultModelCriteria<M> optimize() {
        return flashToModelCriteriaBuilder(criteria());
    }

    @FunctionalInterface
    public interface AtomicFormulaTester<M> {
        public Boolean test(SearchableModelField<? super M> field, Operator operator, Object[] operatorArguments);
    }

    public DefaultModelCriteria<M> partiallyEvaluate(AtomicFormulaTester<M> tester) {
        return new DefaultModelCriteria<>(node.cloneTree((field, operator, operatorArguments) -> {
            Boolean res = tester.test(field, operator, operatorArguments);
            if (res == null) {
                return new ModelCriteriaNode<>(field, operator, operatorArguments);
            } else {
                return new ModelCriteriaNode<>(res ? ExtOperator.__TRUE__ : ExtOperator.__FALSE__);
            }
        }, ModelCriteriaNode::new));
    }

    public boolean isEmpty() {
        return node == null;
    }

    @Override
    public String toString() {
        return isEmpty() ? "" : node.toString();
    }

    private Stream<ModelCriteriaNode<M>> getNodesToAddForAndOr(ModelCriteriaNode<M> nodeToAdd, ExtOperator operatorBeingAdded) {
        final ExtOperator op = nodeToAdd.getNodeOperator();

        if (op == operatorBeingAdded) {
            return nodeToAdd.getChildren().stream();
        }

        return Stream.of(nodeToAdd);
    }

}
