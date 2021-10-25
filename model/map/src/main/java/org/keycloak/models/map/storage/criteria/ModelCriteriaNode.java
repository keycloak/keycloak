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

/**
 *
 * @author hmlnarik
 */
public class ModelCriteriaNode<M> extends DefaultTreeNode<ModelCriteriaNode<M>> {

    public static enum ExtOperator {
        AND {
            @Override public <M> ModelCriteriaBuilder<M> apply(ModelCriteriaBuilder<M> mcb, ModelCriteriaNode<M> node) {
                if (node.getChildren().isEmpty()) {
                    return null;
                }
                final ModelCriteriaBuilder[] operands = node.getChildren().stream()
                  .map(n -> n.flashToModelCriteriaBuilder(mcb))
                  .filter(Objects::nonNull)
                  .toArray(ModelCriteriaBuilder[]::new);
                return operands.length == 0 ? null : mcb.and(operands);
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "(" + node.getChildren().stream().map(ModelCriteriaNode::toString).collect(Collectors.joining(" && ")) + ")";
            }
        },
        OR {
            @Override public <M> ModelCriteriaBuilder<M> apply(ModelCriteriaBuilder<M> mcb, ModelCriteriaNode<M> node) {
                if (node.getChildren().isEmpty()) {
                    return null;
                }
                final ModelCriteriaBuilder[] operands = node.getChildren().stream()
                  .map(n -> n.flashToModelCriteriaBuilder(mcb))
                  .filter(Objects::nonNull)
                  .toArray(ModelCriteriaBuilder[]::new);
                return operands.length == 0 ? null : mcb.or(operands);
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "(" + node.getChildren().stream().map(ModelCriteriaNode::toString).collect(Collectors.joining(" || ")) + ")";
            }
        },
        NOT {
            @Override public <M> ModelCriteriaBuilder<M> apply(ModelCriteriaBuilder<M> mcb, ModelCriteriaNode<M> node) {
                return mcb.not(node.getChildren().iterator().next().flashToModelCriteriaBuilder(mcb));
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return "! " + node.getChildren().iterator().next().toString();
            }
        },
        SIMPLE_OPERATOR {
            @Override public <M> ModelCriteriaBuilder<M> apply(ModelCriteriaBuilder<M> mcb, ModelCriteriaNode<M> node) {
                return mcb.compare(
                  node.field,
                  node.simpleOperator,
                  node.simpleOperatorArguments
                );
            }
            @Override public String toString(ModelCriteriaNode<?> node) {
                return node.field.getName() + " " + node.simpleOperator + " " + Arrays.deepToString(node.simpleOperatorArguments);
            }
        },
        ;

        public abstract <M> ModelCriteriaBuilder<M> apply(ModelCriteriaBuilder<M> mcbCreator, ModelCriteriaNode<M> node);
        public abstract String toString(ModelCriteriaNode<?> node);
    }

    private final ExtOperator nodeOperator;

    private final Operator simpleOperator;

    private final SearchableModelField<M> field;

    private final Object[] simpleOperatorArguments;

    public ModelCriteriaNode(SearchableModelField<M> field, Operator simpleOperator, Object... simpleOperatorArguments) {
        super(Collections.emptyMap());
        this.simpleOperator = simpleOperator;
        this.field = field;
        this.simpleOperatorArguments = simpleOperatorArguments;
        this.nodeOperator = ExtOperator.SIMPLE_OPERATOR;
    }

    public ModelCriteriaNode(ExtOperator nodeOperator) {
        super(Collections.emptyMap());
        this.nodeOperator = nodeOperator;
        this.simpleOperator = null;
        this.field = null;
        this.simpleOperatorArguments = null;
    }

    private ModelCriteriaNode(ExtOperator nodeOperator, Operator simpleOperator, SearchableModelField<M> field, Object[] simpleOperatorArguments) {
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
        return cloneTree(n -> new ModelCriteriaNode<>(n.nodeOperator, n.simpleOperator, n.field, n.simpleOperatorArguments));
    }

    public ModelCriteriaBuilder<M> flashToModelCriteriaBuilder(ModelCriteriaBuilder<M> mcb) {
        final ModelCriteriaBuilder<M> res = nodeOperator.apply(mcb, this);
        return res == null ? mcb : res;
    }

    @Override
    public String toString() {
        return nodeOperator.toString(this);
    }

}
