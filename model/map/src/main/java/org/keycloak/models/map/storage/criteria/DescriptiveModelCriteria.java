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
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Descriptive model criteria implementation which in other words represents a Boolean formula on searchable fields.
 * @author hmlnarik
 */
public abstract class DescriptiveModelCriteria<M, Self extends DescriptiveModelCriteria<M, Self>> implements ModelCriteriaBuilder<M, Self> {

    protected final ModelCriteriaNode<M> node;

    protected DescriptiveModelCriteria(ModelCriteriaNode<M> node) {
        this.node = node;
    }

    @Override
    public Self compare(SearchableModelField<? super M> modelField, Operator op, Object... value) {
        return compare(new ModelCriteriaNode<>(modelField, op, value));
    }

    private Self compare(final ModelCriteriaNode<M> nodeToAdd) {
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

        return instantiateForNode(targetNode);
    }

    protected abstract Self instantiateForNode(ModelCriteriaNode<M> targetNode);

    @Override
    public Self and(Self... mcbs) {
        if (mcbs.length == 1) {
            return compare(mcbs[0].node);
        }

        final ModelCriteriaNode<M> targetNode = new ModelCriteriaNode<>(ExtOperator.AND);
        AtomicBoolean hasFalseNode = new AtomicBoolean(false);
        for (Self mcb : mcbs) {
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
    public Self or(Self... mcbs) {
        if (mcbs.length == 1) {
            return compare(mcbs[0].node);
        }

        final ModelCriteriaNode<M> targetNode = new ModelCriteriaNode<>(ExtOperator.OR);
        AtomicBoolean hasTrueNode = new AtomicBoolean(false);
        for (Self mcb : mcbs) {
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
    public Self not(Self mcb) {
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

    @FunctionalInterface
    public interface AtomicFormulaTester<M> {
        public Boolean test(SearchableModelField<? super M> field, Operator operator, Object[] operatorArguments);
    }

    public Self partiallyEvaluate(AtomicFormulaTester<M> tester) {
        return instantiateForNode(node.cloneTree((field, operator, operatorArguments) -> {
            Boolean res = tester.test(field, operator, operatorArguments);
            if (res == null) {
                return new ModelCriteriaNode<>(field, operator, operatorArguments);
            } else {
                return new ModelCriteriaNode<>(res ? ExtOperator.__TRUE__ : ExtOperator.__FALSE__);
            }
        }, ModelCriteriaNode::new));
    }

    /**
     * Optimizes this formula into another {@code ModelCriteriaBuilder}, using the values of
     * {@link ExtOperator#__TRUE__} and {@link ExtOperator#__FALSE__} accordingly.
     * @return New instance of {@code }
     */
    public Self optimize() {
        return flashToModelCriteriaBuilder(instantiateForNode(null));
    }

    /**
     * Returns the realm ID which limits the results of this criteria.
     * Does not support formulae which include negation of a condition containing the given field.
     * Only supports plain equality ({@link Operator#EQ}), ignores all
     * instances of the field comparison which do not use plain equality.
     * @return {@code null} if the field is not contained in the formula, there are multiple
     *   mutually different field values in the formula, or the formula contains field check within
     *   a negation.
     */
    public <T extends DescriptiveModelCriteria<?, ?>> Object getSingleRestrictionArgument(String fieldName) {
        if (node == null) {
            return null;
        }

        // relax all conditions but those which check realmId equality. For this moment,
        // other operators like NE or IN are disregarded and will be added only if need
        // arises, since the current queries do not use them.
        DescriptiveModelCriteria<M, ?> criterionFormula =
          instantiateForNode(node.cloneTree(n -> {
            switch (n.getNodeOperator()) {
                case ATOMIC_FORMULA:
                    if (fieldName.equals(n.getField().getName()) && n.getSimpleOperator() == Operator.EQ) {
                        return new ModelCriteriaNode<>(n.getField(), n.getSimpleOperator(), n.getSimpleOperatorArguments());
                    }
                    return getNotParentsParity(n.getParent(), true) 
                      ? new ModelCriteriaNode<>(ExtOperator.__TRUE__)
                      : new ModelCriteriaNode<>(ExtOperator.__FALSE__);
                default:
                    return new ModelCriteriaNode<>(n.getNodeOperator());
            }
          }))
          .optimize();

        final ModelCriteriaNode<M> criterionFormulaRoot = criterionFormula.getNode();
        if (criterionFormulaRoot.isFalseNode()) {
            return null;
        }

        if (criterionFormulaRoot.isTrueNode()) {
            return null;
        }

        ThreadLocal<Object> criterionArgument = new ThreadLocal<>();
        @SuppressWarnings("unchecked")
        Optional<ModelCriteriaNode<M>> firstInvalidNode = criterionFormulaRoot.findFirstDfs(n -> {
            switch (n.getNodeOperator()) {
                case NOT:
                    return true;

                case ATOMIC_FORMULA:    // Atomic formula must be of the form "realmID" EQ ..., see realmIdFormula instatiation
                    Object argument = getSingleArgument(n.getSimpleOperatorArguments());
                    if (argument != null) {
                        Object orig = criterionArgument.get();
                        if (orig != null && ! Objects.equals(argument, orig)) {
                            // Two different realms are not supported
                            return true;
                        }
                        criterionArgument.set(argument);
                    }
                    return false;

                default:
                    return false;
            }
        });

        return firstInvalidNode.isPresent() ? null : criterionArgument.get();
    }

    private static Object getSingleArgument(Object[] arguments) {
        if (arguments == null || arguments.length != 1) {
            return null;
        }

        final Object a0 = arguments[0];
        if (a0 instanceof Collection) { // Note this cannot be a Stream due to ModelCriteriaNode always converting stream to List
            final Collection c0 = (Collection) a0;
            return c0.size() == 1 ? c0.iterator().next() : null;
        }

        return a0;
    }

    public boolean isEmpty() {
        return node == null;
    }

    public ModelCriteriaNode<M> getNode() {
        return node;
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

    private boolean getNotParentsParity(Optional<ModelCriteriaNode<M>> node, boolean currentValue) {
        return node
          .map(n -> getNotParentsParity(n.getParent(), n.getNodeOperator() == ExtOperator.NOT ? ! currentValue : currentValue))
          .orElse(currentValue);
    }

}
