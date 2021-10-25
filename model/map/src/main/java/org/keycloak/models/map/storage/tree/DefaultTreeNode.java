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

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic implementation of a node in a tree.
 * <p>
 * Any method that is not purely on tree or nodes should go into a specialized subclass of this class!
 *
 * @author hmlnarik
 */
public class DefaultTreeNode<Self extends DefaultTreeNode<Self>> implements TreeNode<Self> {

    private final Map<String, Object> nodeProperties;
    private final Map<String, Object> edgeProperties;
    private final Map<String, Object> treeProperties;
    private final LinkedList<Self> children = new LinkedList<>();
    private String id;
    private Self parent;

    /**
     * @param treeProperties Reference to tree properties map. Tree properties are maintained outside of this node.
     */
    protected DefaultTreeNode(Map<String, Object> treeProperties) {
        this.treeProperties = treeProperties;
        this.edgeProperties = new HashMap<>();
        this.nodeProperties = new HashMap<>();
    }

    public DefaultTreeNode(Map<String, Object> nodeProperties, Map<String, Object> edgeProperties, Map<String, Object> treeProperties) {
        this.nodeProperties = nodeProperties;
        this.edgeProperties = edgeProperties;
        this.treeProperties = treeProperties;
    }

    @Override
    public Map<String, Object> getEdgeProperties() {
        return this.edgeProperties;
    }

    @Override
    public <V> Optional<V> getEdgeProperty(String key, Class<V> clazz) {
        final Object v = getEdgeProperties().get(key);
        return clazz.isInstance(v) ? Optional.of(clazz.cast(v)) : Optional.empty();
    }

    public void setEdgeProperty(String property, Object value) {
        this.edgeProperties.put(property, value);
    }

    @Override
    public Map<String, Object> getNodeProperties() {
        return this.nodeProperties;
    }

    @Override
    public <V> Optional<V> getNodeProperty(String key, Class<V> clazz) {
        final Object v = getNodeProperties().get(key);
        return clazz.isInstance(v) ? Optional.of(clazz.cast(v)) : Optional.empty();
    }

    public void setNodeProperty(String property, Object value) {
        this.nodeProperties.put(property, value);
    }

    @Override
    public Map<String, Object> getTreeProperties() {
        return this.treeProperties;
    }

    @Override
    public <V> Optional<V> getTreeProperty(String key, Class<V> clazz) {
        final Object v = getTreeProperties().get(key);
        return clazz.isInstance(v) ? Optional.of(clazz.cast(v)) : Optional.empty();
    }

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Optional<Self> findFirstDfs(Predicate<Self> visitor) {
        Deque<Self> stack = new LinkedList<>();
        stack.add(getThis());
        while (! stack.isEmpty()) {
            Self node = stack.pop();
            if (visitor.test(node)) {
                return Optional.of(node);
            }
            List<Self> c = node.getChildren();
            for (ListIterator<Self> li = c.listIterator(c.size()); li.hasPrevious(); ) {
                stack.push(li.previous());
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Self> findFirstBottommostDfs(Predicate<Self> visitor) {
        Deque<Self> stack = new LinkedList<>();
        stack.add(getThis());
        while (! stack.isEmpty()) {
            Self node = stack.pop();
            if (visitor.test(node)) {
                // find the bottommost, i.e. inspect children first before returning this node
                for (Self child : node.getChildren()) {
                    Optional<Self> childRes = child.findFirstBottommostDfs(visitor);
                    if (childRes.isPresent()) {
                        return childRes;
                    }
                }
                return Optional.of(node);
            }
            List<Self> c = node.getChildren();
            for (ListIterator<Self> li = c.listIterator(c.size()); li.hasPrevious(); ) {
                stack.push(li.previous());
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Self> findFirstBfs(Predicate<Self> visitor) {
        Queue<Self> queue = new LinkedList<>();
        queue.add(getThis());
        while (! queue.isEmpty()) {
            Self node = queue.poll();
            if (visitor.test(node)) {
                return Optional.of(node);
            }
            queue.addAll(node.getChildren());
        }

        return Optional.empty();
    }

    @Override
    public List<Self> getPathToRoot(PathOrientation orientation) {
        LinkedList<Self> res = new LinkedList<>();
        Consumer<Self> addFunc = orientation == PathOrientation.BOTTOM_FIRST ? res::addLast : res::addFirst;
        Optional<Self> p = Optional.of(getThis());
        while (p.isPresent()) {
            addFunc.accept(p.get());
            p = p.get().getParent();
        }
        return res;
    }

    @Override
    public List<Self> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    @Override
    public void addChild(Self node) {
        if (node == null) {
            return;
        }
        if (! this.children.contains(node)) {
            this.children.add(node);
        }
        node.setParent(getThis());

        // Prevent setting a parent of this node as a child of this node. In such a case, remove the parent of this node
        for (Optional<Self> p = getParent(); p.isPresent(); p = p.get().getParent()) {
            if (p.get() == node) {
                setParent(null);
                return;
            }
        }
    }

    @Override
    public void addChild(int index, Self node) {
        if (node == null) {
            return;
        }
        if (! this.children.contains(node)) {
            this.children.add(index, node);
        }
        node.setParent(getThis());

        // Prevent setting a parent of this node as a child of this node. In such a case, remove the parent of this node
        for (Optional<Self> p = getParent(); p.isPresent(); p = p.get().getParent()) {
            if (p.get() == node) {
                setParent(null);
                return;
            }
        }
    }

    @Override
    public Optional<Self> getChild(String id) {
        for (Self c : children) {
            if (id.equals(c.getId())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Override
    public int removeChild(Predicate<Self> shouldRemove) {
        if (shouldRemove == null) {
            return 0;
        }
        int res = 0;
        for (Iterator<Self> it = children.iterator(); it.hasNext();) {
            Self n = it.next();
            if (shouldRemove.test(n)) {
                it.remove();
                n.setParent(null);
                res++;
            }
        }
        return res;
    }

    @Override
    public Optional<Self> removeChild(Self node) {
        if (node == null) {
            return Optional.empty();
        }
        for (Iterator<Self> it = children.iterator(); it.hasNext();) {
            Self res = it.next();
            if (node.equals(res)) {
                it.remove();
                res.setParent(null);
                return Optional.of(res);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Self> getParent() {
        return Optional.ofNullable(this.parent);
    }

    @Override
    public void setParent(Self parent) {
        if (this.parent == parent) {
            return;
        }
        if (parent == this) {
            setParent(null);
        }

        if (this.parent != null) {
            Self previousParent = this.parent;
            this.parent = null;
            previousParent.removeChild(getThis());
        }

        if (parent != null) {
            this.parent = parent;
            parent.addChild(getThis());
        }
    }

    public <RNode extends TreeNode<? super RNode>> RNode cloneTree(Function<Self, RNode> instantiateFunc) {
        final RNode res = instantiateFunc.apply(getThis());
        this.getChildren().forEach(c -> res.addChild(c.cloneTree(instantiateFunc)));
        return res;
    }

    @SuppressWarnings("unchecked")
    private Self getThis() {
        return (Self) this;
    }
}
