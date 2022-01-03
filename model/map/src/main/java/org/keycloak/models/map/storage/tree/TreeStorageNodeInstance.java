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
package org.keycloak.models.map.storage.tree;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.storage.tree.TreeStorageNodePrescription.FieldContainedStatus;

/**
 * Instance of the tree storage that is based on a prescription ({@link TreeStorageNodePrescription}),
 * i.e. it provides a map storage instance that can be used for accessing data.
 *
 * @author hmlnarik
 */
public class TreeStorageNodeInstance<V extends AbstractEntity>
  extends DefaultTreeNode<TreeStorageNodeInstance<V>> {

    private final KeycloakSession session;
    private final TreeStorageNodePrescription prescription;
    private final TreeStorageNodeInstance<V> original;   // If this node is a subtree, keep reference to the node in the original tree here

    public class WithEntity {
        private final V entity;

        public WithEntity(V entity) {
            this.entity = entity;
        }

        public V getEntity() {
            return entity;
        }

        public TreeStorageNodeInstance<V> getNode() {
            return TreeStorageNodeInstance.this;
        }

    }

    public TreeStorageNodeInstance(KeycloakSession session, TreeStorageNodeInstance<V> original) {
        super(original.getNodeProperties(), original.getEdgeProperties(), original.getTreeProperties());
        this.original = original;
        this.prescription = original.prescription;
        this.session = session;
    }

    public TreeStorageNodeInstance(KeycloakSession session, TreeStorageNodePrescription prescription) {
        super(
          prescription.getNodeProperties() == null ? null : new HashMap<>(prescription.getNodeProperties()),
          prescription.getEdgeProperties() == null ? null : new HashMap<>(prescription.getEdgeProperties()),
          prescription.getTreeProperties()
        );
        this.prescription = prescription;
        this.session = session;
        this.original = null;
    }

    public TreeStorageNodeInstance<V> cloneNodeOnly() {
        return new TreeStorageNodeInstance<>(session, this.original == null ? this : this.original);
    }

    @Override
    public String getId() {
        return prescription.getId();
    }

    public boolean isReadOnly() {
        return prescription.isReadOnly();
    }

    public FieldContainedStatus isCacheFor(EntityField<V> field, Object parameter) {
        return prescription.isCacheFor(field, parameter);
    }

    public FieldContainedStatus isPrimarySourceFor(Enum<? extends EntityField<V>> field, Object parameter) {
        return prescription.isPrimarySourceFor((EntityField<?>) field, parameter);
    }

}
