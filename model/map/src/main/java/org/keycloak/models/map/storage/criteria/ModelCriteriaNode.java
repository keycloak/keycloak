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
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.tree.DefaultTreeNode;
import org.keycloak.storage.SearchableModelField;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO: Introduce separation of parameter values and the structure
 * @author hmlnarik
 */
public class ModelCriteriaNode<M> extends DefaultTreeNode<ModelCriteriaNode<M>> {

    public static enum ExtOperator {
        AND {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                if (node.hasNoChildren()) {
                    return null;
                }
                final C[] operands = node.getChildren().stream()
                  .map(n -> n.flashToModelCriteriaBuilder(mcb))
                  .filter(Objects::nonNull)
                  .toArray(mcb::newArray);
                return operands.length == 0 ? null :
                  operands.length == 1 ? operands[0] : mcb.and(operands);
            }
            @Override public <M> ModelCriteriaNode<M> evaluate(ModelCriteriaNode<M> node, AtomicFormulaInstantiator<M> atomicFormulaInstantiator) {
                ModelCriteriaNode<M> res = andNode();
                int i = 0;
                for (ModelCriteriaNode<M> child : node.getChildren()) {
                    child = child.partiallyEvaluate(atomicFormulaInstantiator);
                    switch (child.getNodeOperator()) {
                        case __FALSE__:
                            return falseNode();
                        case __TRUE__:
                            break;
                        default:
                            res.addChild(child);
                            i++;
                    }
                }
                return i == 0 ? trueNode() :
                  (i == 1 ? res.getChildren().get(0) : res);
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "(" + node.getChildren().stream().map(ModelCriteriaNode::toString).collect(Collectors.joining(" && ")) + ")";
            }
        },
        OR {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                if (node.hasNoChildren()) {
                    return null;
                }
                final C[] operands = node.getChildren().stream()
                  .map(n -> n.flashToModelCriteriaBuilder(mcb))
                  .filter(Objects::nonNull)
                  .toArray(mcb::newArray);
                return operands.length == 0 ? null :
                  operands.length == 1 ? operands[0] : mcb.or(operands);
            }
            @Override public <M> ModelCriteriaNode<M> evaluate(ModelCriteriaNode<M> node, AtomicFormulaInstantiator<M> atomicFormulaInstantiator) {
                ModelCriteriaNode<M> res = orNode();
                int i = 0;
                for (ModelCriteriaNode<M> child : node.getChildren()) {
                    child = child.partiallyEvaluate(atomicFormulaInstantiator);
                    switch (child.getNodeOperator()) {
                        case __FALSE__:
                            break;
                        case __TRUE__:
                            return trueNode();
                        default:
                            res.addChild(child);
                            i++;
                    }
                }
                return i == 0 ? falseNode() :
                  (i == 1 ? res.getChildren().get(0) : res);
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "(" + node.getChildren().stream().map(ModelCriteriaNode::toString).collect(Collectors.joining(" || ")) + ")";
            }
        },
        NOT {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                return mcb.not(node.getChildren().iterator().next().flashToModelCriteriaBuilder(mcb));
            }
            @Override public <M> ModelCriteriaNode<M> evaluate(ModelCriteriaNode<M> node, AtomicFormulaInstantiator<M> atomicFormulaInstantiator) {
                ModelCriteriaNode<M> child = node.getChildren().get(0).partiallyEvaluate(atomicFormulaInstantiator);
                switch (child.getNodeOperator()) {
                    case __FALSE__:
                        return trueNode();
                    case __TRUE__:
                        return falseNode();
                    default:
                        ModelCriteriaNode<M> res = notNode();
                        res.addChild(child);
                        return res;
                }
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "! " + node.getChildren().get(0);
            }
        },
        ATOMIC_FORMULA {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                return (C) mcb.compare(
                  node.field,
                  node.simpleOperator,
                  node.simpleOperatorArguments
                );
            }
            @Override public <M> ModelCriteriaNode<M> evaluate(ModelCriteriaNode<M> node, AtomicFormulaInstantiator<M> atomicFormulaInstantiator) {
                return atomicFormulaInstantiator.instantiateAtomicFormula(node);
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return node.field.getName() + " " + node.simpleOperator + " " + Arrays.deepToString(node.simpleOperatorArguments);
            }
        },
        __FALSE__ {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                return mcb.or(mcb.newArray(0));
            }
            @Override public <M> ModelCriteriaNode<M> evaluate(ModelCriteriaNode<M> node, AtomicFormulaInstantiator<M> atomicFormulaInstantiator) {
                return node;
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "__FALSE__";
            }
        },
        __TRUE__ {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                return mcb.and(mcb.newArray(0));
            }
            @Override public <M> ModelCriteriaNode<M> evaluate(ModelCriteriaNode<M> node, AtomicFormulaInstantiator<M> atomicFormulaInstantiator) {
                return node;
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "__TRUE__";
            }
        }
        ;

        public abstract <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcbCreator, ModelCriteriaNode<M> node);
        public abstract String toString(ModelCriteriaNode<?> node);
        public abstract <M> ModelCriteriaNode<M> evaluate(ModelCriteriaNode<M> node, AtomicFormulaInstantiator<M> atomicFormulaInstantiator);
    }

    protected final ExtOperator nodeOperator;

    protected final Operator simpleOperator;

    protected final SearchableModelField<? super M> field;

    protected final Object[] simpleOperatorArguments;

    public static <M> ModelCriteriaNode<M> atomicFormula(SearchableModelField<? super M> field, Operator simpleOperator, Object[] simpleOperatorArguments) {
        return new ModelCriteriaNode<>(field, simpleOperator, simpleOperatorArguments);
    }

    public static <M> ModelCriteriaNode<M> atomicFormula(ModelCriteriaNode<M> node) {
        return new ModelCriteriaNode<>(node.field, node.simpleOperator, node.simpleOperatorArguments);
    }

    @SuppressWarnings("unchecked")
    public static <M> ModelCriteriaNode<M> trueNode() {
        return (ModelCriteriaNode<M>) new ModelCriteriaNode<>(ExtOperator.__TRUE__);
    }

    @SuppressWarnings("unchecked")
    public static <M> ModelCriteriaNode<M> falseNode() {
        return (ModelCriteriaNode<M>) new ModelCriteriaNode<>(ExtOperator.__FALSE__);
    }

    public static <M> ModelCriteriaNode<M> andNode() {
        return new ModelCriteriaNode<>(ExtOperator.AND);
    }

    public static <M> ModelCriteriaNode<M> orNode() {
        return new ModelCriteriaNode<>(ExtOperator.OR);
    }

    public static <M> ModelCriteriaNode<M> notNode() {
        return new ModelCriteriaNode<>(ExtOperator.NOT);
    }

    public ModelCriteriaNode<M> cloneNode() {
        if (this.nodeOperator == ExtOperator.ATOMIC_FORMULA) {
            return atomicFormula(this);
        } else {
            return new ModelCriteriaNode<>(this.nodeOperator);
        }
    }

    protected ModelCriteriaNode(SearchableModelField<? super M> field, Operator simpleOperator, Object[] simpleOperatorArguments) {
        super(Collections.emptyMap());
        this.simpleOperator = simpleOperator;
        this.field = field;
        this.simpleOperatorArguments = simpleOperatorArguments;
        this.nodeOperator = ExtOperator.ATOMIC_FORMULA;

        if (simpleOperatorArguments != null) {
            for (int i = 0; i < simpleOperatorArguments.length; i ++) {
                Object arg = simpleOperatorArguments[i];
                if (arg instanceof Stream) {
                    try (Stream<?> sArg = (Stream<?>) arg) {
                        simpleOperatorArguments[i] = sArg.collect(Collectors.toList());
                    }
                }
            }
        }
    }

    protected ModelCriteriaNode(ExtOperator nodeOperator) {
        super(Collections.emptyMap());
        this.nodeOperator = nodeOperator;
        this.simpleOperator = null;
        this.field = null;
        this.simpleOperatorArguments = null;
    }

    public ExtOperator getNodeOperator() {
        return nodeOperator;
    }

    public ModelCriteriaNode<M> cloneTree() {
        return partiallyEvaluate(ModelCriteriaNode::atomicFormula);
    }

    @FunctionalInterface
    public interface AtomicFormulaInstantiator<M> {
        /**
         * Instantiates a {@link ModelCriteriaNode} for a particular atomic formula.
         * <p>
         * Note: the instantiation can evaluate the formula by returning {@link ModelCriteriaNode#trueNode()}
         *       or {@link ModelCriteriaNode#falseNode()}.
         *
         * @param field
         * @param operator
         * @param operatorArguments
         * @return
         */
        ModelCriteriaNode<M> instantiateAtomicFormula(ModelCriteriaNode<M> node);
    }

    public ModelCriteriaNode<M> partiallyEvaluate(AtomicFormulaInstantiator<M> atomicFormulaInstantiator) {
        return nodeOperator.evaluate(this, atomicFormulaInstantiator);
    }

    public boolean isFalseNode() {
        return getNodeOperator() == ExtOperator.__FALSE__;
    }

    public boolean isNotFalseNode() {
        return getNodeOperator() != ExtOperator.__FALSE__;
    }

    public boolean isTrueNode() {
        return getNodeOperator() == ExtOperator.__TRUE__;
    }

    public boolean isNotTrueNode() {
        return getNodeOperator() != ExtOperator.__TRUE__;
    }

    public SearchableModelField<? super M> getField() {
        return field;
    }

    public Operator getSimpleOperator() {
        return simpleOperator;
    }

    public Object[] getSimpleOperatorArguments() {
        return simpleOperatorArguments;
    }

    public <C extends ModelCriteriaBuilder<M, C>> C flashToModelCriteriaBuilder(C mcb) {
        final C res = getNodeOperator().apply(mcb, this);
        return res == null ? mcb : res;
    }

    @Override
    public String toString() {
        return nodeOperator.toString(this);
    }

}
