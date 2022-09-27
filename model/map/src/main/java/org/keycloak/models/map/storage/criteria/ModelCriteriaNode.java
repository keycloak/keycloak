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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
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
                if (node.getChildren().isEmpty()) {
                    return null;
                }
                final C[] operands = node.getChildren().stream()
                  .map(n -> n.flashToModelCriteriaBuilder(mcb))
                  .filter(Objects::nonNull)
                  .toArray(n -> (C[]) Array.newInstance(mcb.getClass(), n));
                return operands.length == 0 ? null : mcb.and(operands);
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "(" + node.getChildren().stream().map(ModelCriteriaNode::toString).collect(Collectors.joining(" && ")) + ")";
            }
        },
        OR {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                if (node.getChildren().isEmpty()) {
                    return null;
                }
                final C[] operands = node.getChildren().stream()
                  .map(n -> n.flashToModelCriteriaBuilder(mcb))
                  .filter(Objects::nonNull)
                  .toArray(n -> (C[]) Array.newInstance(mcb.getClass(), n));
                return operands.length == 0 ? null : mcb.or(operands);
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "(" + node.getChildren().stream().map(ModelCriteriaNode::toString).collect(Collectors.joining(" || ")) + ")";
            }
        },
        NOT {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                return mcb.not(node.getChildren().iterator().next().flashToModelCriteriaBuilder(mcb));
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "! " + node.getChildren().iterator().next().toString();
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
            @Override public String toString(ModelCriteriaNode<?> node) {
                return node.field.getName() + " " + node.simpleOperator + " " + Arrays.deepToString(node.simpleOperatorArguments);
            }
        },
        __FALSE__ {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                return mcb.or((C[]) Array.newInstance(mcb.getClass(), 0));
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "__FALSE__";
            }
        },
        __TRUE__ {
            @Override public <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcb, ModelCriteriaNode<M> node) {
                return mcb.and((C[]) Array.newInstance(mcb.getClass(), 0));
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "__TRUE__";
            }
        }
        ;

        public abstract <M, C extends ModelCriteriaBuilder<M, C>> C apply(C mcbCreator, ModelCriteriaNode<M> node);
        public abstract String toString(ModelCriteriaNode<?> node);
    }

    private final ExtOperator nodeOperator;

    private final Operator simpleOperator;

    private final SearchableModelField<? super M> field;

    private final Object[] simpleOperatorArguments;

    public ModelCriteriaNode(SearchableModelField<? super M> field, Operator simpleOperator, Object[] simpleOperatorArguments) {
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

    public ModelCriteriaNode(ExtOperator nodeOperator) {
        super(Collections.emptyMap());
        this.nodeOperator = nodeOperator;
        this.simpleOperator = null;
        this.field = null;
        this.simpleOperatorArguments = null;
    }

    private ModelCriteriaNode(ExtOperator nodeOperator, Operator simpleOperator, SearchableModelField<? super M> field, Object[] simpleOperatorArguments) {
        super(Collections.emptyMap());
        this.nodeOperator = nodeOperator;
        this.simpleOperator = simpleOperator;
        this.field = field;
        this.simpleOperatorArguments = simpleOperatorArguments;
    }

    public ExtOperator getNodeOperator() {
        return nodeOperator;
    }

    public ModelCriteriaNode<M> cloneTree() {
        return cloneTree(ModelCriteriaNode::new, ModelCriteriaNode::new);
    }

    @FunctionalInterface
    public interface AtomicFormulaInstantiator<M> {
        public ModelCriteriaNode<M> instantiate(SearchableModelField<? super M> field, Operator operator, Object[] operatorArguments);
    }

    public ModelCriteriaNode<M> cloneTree(AtomicFormulaInstantiator<M> atomicFormulaInstantiator, Function<ExtOperator, ModelCriteriaNode<M>> booleanNodeInstantiator) {
        return cloneTree(n -> 
          n.getNodeOperator() == ExtOperator.ATOMIC_FORMULA
            ? atomicFormulaInstantiator.instantiate(n.field, n.simpleOperator, n.simpleOperatorArguments)
            : booleanNodeInstantiator.apply(n.nodeOperator)
        );
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

    public <C extends ModelCriteriaBuilder<M, C>> C flashToModelCriteriaBuilder(C mcb) {
        final C res = nodeOperator.apply(mcb, this);
        return res == null ? mcb : res;
    }

    @Override
    public String toString() {
        return nodeOperator.toString(this);
    }

}
