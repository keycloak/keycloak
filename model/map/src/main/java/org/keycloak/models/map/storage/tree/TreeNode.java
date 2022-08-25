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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Interface representing a node in a tree that has ID.
 * <p>
 * Think twice when adding a method here: if added method does not operate purely 
 * on nodes or a generic tree, it does not belong here.
 * @author hmlnarik
 */
public interface TreeNode<Self extends TreeNode<? extends Self>> {

    public enum PathOrientation { BOTTOM_FIRST, TOP_FIRST }

    /**
     * Adds a node as a child of this node, and sets the parent of the {@code node} to this node.
     * @param node Future child node. If {@code null} or the node is already amongst the children list, no action is done.
     */
    void addChild(Self node);

    /**
     * Adds a node as a child of this node, and sets the parent of the {@code node} to this node.
     * @param index Index at which the specified element is to be inserted
     * @param node Future child node. If {@code null} or the node is already amongst the children list, no action is done.
     * 
     */
    void addChild(int index, Self node);

    /**
     * Returns a node by ID. If there are more nodes with the same ID, any node from those may be returned.
     * @param id
     * @return
     */
    Optional<Self> getChild(String id);

    /**
     * Returns the children of the current node. Order does matter.
     * @return Read-only list of the children. Never returns {@code null}.
     */
    List<Self> getChildren();

    /**
     * Parent-to-this-node edge properties. For example, import/no-import mode or sync mode belongs here.
     * @return Returns properties of the edge from the parent to this node. Never returns {@code null}.
     */
    Map<String, Object> getEdgeProperties();

    /**
     * Convenience method for obtaining a single parent-to-this-node edge property.
     * @param <V>
     * @param key
     * @param clazz
     * @return {@code Optional} with a property value if it exists. Never returns {@code null}
     */
    <V> Optional<V> getEdgeProperty(String key, Class<V> clazz);

    /**
     * Returns ID of the node, which could match e.g. ID of the component with storage definition.
     * @return Node ID
     */
    String getId();

    /**
     * Properties of the this node. In storage context, properties of the single map storage represented by this
     * node, for example read-only/read-write flag.
     * @return Returns properties of the storage managed in this node. Never returns {@code null}.
     */
    Map<String, Object> getNodeProperties();

    /**
     * Convenience method for obtaining a single property of this node.
     * @param <V>
     * @param key
     * @param clazz
     * @return {@code Optional} with a property value if it exists. Never returns {@code null}
     */
    <V> Optional<V> getNodeProperty(String key, Class<V> clazz);

    /**
     * Properties of the whole tree. For example, kind of the stored objects, e.g. realms or clients.
     * @return Returns properties of the tree that contains in this node. Never returns {@code null}.
     */
    Map<String, Object> getTreeProperties();

    /**
     * Convenience method for obtaining a single property of tree that this node belongs to.
     * @param <V>
     * @param key
     * @param clazz
     * @return {@code Optional} with a property value if it exists. Never returns {@code null}
     */
    <V> Optional<V> getTreeProperty(String key, Class<V> clazz);

    /**
     * Removes the given child node.
     * @param node Node to remove
     * @return Removed node
     */
    Optional<Self> removeChild(Self node);

    /**
     * Removes child nodes satisfying the given predicate.
     * @param node Predicate on node returning {@code true} for each node that should be removed
     * @return Number of removed nodes
     */
    int removeChild(Predicate<Self> shouldRemove);

    /**
     * Returns parent node or an empty {@code Optional} if this node is a root node.
     * @return See description. Never returns {@code null}.
     */
    Optional<Self> getParent();

    /**
     * Sets the parent node to the given {@code parent}. If this node was a child of another node,
     * also removes this node from the children of the previous parent.
     * @param parent New parent node or {@code null} if this node should be parentless.
     */
    void setParent(Self parent);

    /**
     * Depth-first search for a node.
     * @param visitor Predicate on nodes, returns {@code true} when a search condition is satisfied which terminates the search.
     * @return Leftmost first node that matches the predicate, {@code null} when no node matches.
     */
    Optional<Self> findFirstDfs(Predicate<Self> visitor);

    /**
     * Depth-first search for a node that is bottommost from those matching DFS.
     * @param visitor Predicate on nodes, returns {@code true} when a search condition is satisfied which terminates the search.
     * @return Leftmost and bottommost node that matches the predicate, {@code null} when no node matches.
     */
    Optional<Self> findFirstBottommostDfs(Predicate<Self> visitor);

    /**
     * Breadth-first search for a node.
     * @param visitor Predicate on nodes, returns {@code true} when a search condition is satisfied which terminates the search.
     * @return First node that matches the predicate, {@code null} when no node matches.
     */
    Optional<Self> findFirstBfs(Predicate<Self> visitor);

    /**
     * Returns the path (list of nodes) from this node to root node.
     * @param orientation Determines order of the nodes in the returned list - either this node is first and the root node
     *   is last, ({@link PathOrientation#BOTTOM_FIRST}) or vice versa ({@link PathOrientation#TOP_FIRST}).
     * @return
     */
    List<Self> getPathToRoot(PathOrientation orientation);

    /**
     * Returns a stream of the nodes laying on the path from this node (exclusive) to the root of the tree (inclusive).
     * @return
     */
    Stream<Self> getParentsStream();

    /**
     * Calls the given {@code visitor} on each node laying on the path from this node (exclusive) to the root of the tree (inclusive).
     * @param visitor
     */
    void forEachParent(Consumer<Self> visitor);

    /**
     * Walks the tree with the given visitor in depth-first search manner.
     * @param visitorUponEntry Visitor called upon entry of the node. May be {@code null}, in that case no action is performed.
     * @param visitorAfterChildrenVisited Visitor called before exit of the node. May be {@code null}, in that case no action is performed.
     */
    void walkDfs(Consumer<Self> visitorUponEntry, Consumer<Self> visitorAfterChildrenVisited);

    /**
     * Walks the tree with the given visitor in breadth-first search manner.
     * @param visitor
     */
    void walkBfs(Consumer<Self> visitor);
}
