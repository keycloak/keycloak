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
import org.keycloak.models.map.storage.criteria.ModelCriteriaNode.AtomicFormulaInstantiator;
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

    @SuppressWarnings("unchecked")
    public static <M> DefaultModelCriteria<M> criteria() {
        return (DefaultModelCriteria<M>) INSTANCE;
    }

    public ModelCriteriaNode<M> getNode() {
        return node;
    }

    @Override
    public DefaultModelCriteria<M> compare(SearchableModelField<? super M> modelField, Operator op, Object... value) {
        return compare(ModelCriteriaNode.atomicFormula(modelField, op, value));
    }

    private DefaultModelCriteria<M> compare(final ModelCriteriaNode<M> nodeToAdd) {
        ModelCriteriaNode<M> targetNode;

        if (isEmpty()) {
            targetNode = nodeToAdd;
        } else if (node.getNodeOperator() == ExtOperator.AND) {
            targetNode = node.cloneTree();
            targetNode.addChild(nodeToAdd);
        } else {
            targetNode = ModelCriteriaNode.andNode();
            targetNode.addChild(node.cloneTree());
            targetNode.addChild(nodeToAdd);
        }

        return new DefaultModelCriteria<>(targetNode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultModelCriteria<M> and(DefaultModelCriteria<M>... mcbs) {
        if (mcbs.length == 1) {
            return compare(mcbs[0].node);
        }

        final ModelCriteriaNode<M> targetNode = ModelCriteriaNode.andNode();
        AtomicBoolean hasFalseNode = new AtomicBoolean(false);
        for (DefaultModelCriteria<M> mcb : mcbs) {
            final ModelCriteriaNode<M> nodeToAdd = mcb.node;
            getNodesToAddForAndOr(nodeToAdd, ExtOperator.AND)
              .filter(ModelCriteriaNode::isNotTrueNode)
              .peek(n -> { if (n.isFalseNode()) hasFalseNode.lazySet(true); })
              .map(ModelCriteriaNode::cloneTree)
              .forEach(targetNode::addChild);

            if (hasFalseNode.get()) {
                return compare(ModelCriteriaNode.falseNode());
            }
        }

        if (targetNode.hasNoChildren()) {
            // AND on empty set of formulae is TRUE: It does hold that there all formulae are satisfied
            return compare(ModelCriteriaNode.trueNode());
        }

        return compare(targetNode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultModelCriteria<M> or(DefaultModelCriteria<M>... mcbs) {
        if (mcbs.length == 1) {
            return compare(mcbs[0].node);
        }

        final ModelCriteriaNode<M> targetNode = ModelCriteriaNode.orNode();
        AtomicBoolean hasTrueNode = new AtomicBoolean(false);
        for (DefaultModelCriteria<M> mcb : mcbs) {
            final ModelCriteriaNode<M> nodeToAdd = mcb.node;
            getNodesToAddForAndOr(nodeToAdd, ExtOperator.OR)
              .filter(ModelCriteriaNode::isNotFalseNode)
              .peek(n -> { if (n.isTrueNode()) hasTrueNode.lazySet(true); })
              .map(ModelCriteriaNode::cloneTree)
              .forEach(targetNode::addChild);

            if (hasTrueNode.get()) {
                return compare(ModelCriteriaNode.trueNode());
            }
        }

        if (targetNode.hasNoChildren()) {
            // OR on empty set of formulae is FALSE: It does not hold that there is at least one satisfied formula
            return compare(ModelCriteriaNode.falseNode());
        }

        return compare(targetNode);
    }

    @Override
    public DefaultModelCriteria<M> not(DefaultModelCriteria<M> mcb) {
        ModelCriteriaNode<M> toBeChild = mcb.node;
        if (toBeChild.getNodeOperator() == ExtOperator.NOT) {
            return compare(toBeChild.getChildren().get(0).cloneTree());
        }

        final ModelCriteriaNode<M> targetNode = ModelCriteriaNode.notNode();
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

    @Override
    @SuppressWarnings("unchecked")
    public DefaultModelCriteria<M>[] newArray(int length) {
        return (DefaultModelCriteria<M>[]) new DefaultModelCriteria[length];
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
        /**
         * Partial function which can evaluate the boolean expression {@code field (operator) operatorArguments}.
         * @param field
         * @param operator
         * @param operatorArguments
         * @return {@code True} or {@code False} if the expression can be evaluated, {@code null} otherwise.
         */
        public Boolean test(SearchableModelField<? super M> field, Operator operator, Object[] operatorArguments);
    }

    public DefaultModelCriteria<M> partiallyEvaluate(AtomicFormulaTester<M> tester) {
        return partiallyEvaluate((AtomicFormulaInstantiator<M>) node -> {
            Boolean res = tester.test(node.field, node.getSimpleOperator(), node.getSimpleOperatorArguments());
            if (res == null) {
                return ModelCriteriaNode.atomicFormula(node);
            } else {
                return res ? ModelCriteriaNode.trueNode() : ModelCriteriaNode.falseNode();
            }
          });
    }

    public DefaultModelCriteria<M> partiallyEvaluate(AtomicFormulaInstantiator<M> transformer) {
        return new DefaultModelCriteria<>(node.partiallyEvaluate(transformer));
    }

    public boolean isEmpty() {
        return node == null;
    }

    public boolean isAlwaysTrue() {
        return node != null && getNode().isTrueNode();
    }

    public boolean isAlwaysFalse() {
        return node != null && getNode().isFalseNode();
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
