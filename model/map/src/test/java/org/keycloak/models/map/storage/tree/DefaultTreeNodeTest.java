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

import org.keycloak.models.map.storage.tree.TreeNode.PathOrientation;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 * @author hmlnarik
 */
public class DefaultTreeNodeTest {

    private class Node extends DefaultTreeNode<Node> {

        public Node() {
            super(DefaultTreeNodeTest.this.treeProperties);
        }

        public Node(String id) {
            super(DefaultTreeNodeTest.this.treeProperties);
            setId(id);
        }

        public Node(Node parent, String id) {
            super(DefaultTreeNodeTest.this.treeProperties);
            setId(id);
            setParent(parent);
        }

        @Override
        public String getLabel() {
            return this.getId() == null ? "Node:" + System.identityHashCode(this) : this.getId();
        }
    }

    private static final String KEY_1 = "key1";
    private static final String VALUE_1 = "value";
    private static final String KEY_2 = "key2";
    private static final Date VALUE_2 = new Date();
    private static final String KEY_3 = "key3";
    private static final Integer VALUE_3 = 12345;

    public Map<String, Object> treeProperties = new HashMap<>();

    {
        treeProperties.put(KEY_1, VALUE_1);
        treeProperties.put(KEY_2, VALUE_2);
    }

    @Test
    public void testSingleNodeTree() {
        Node root = new Node();
        root.setNodeProperty(KEY_1, VALUE_1);
        root.setEdgeProperty(KEY_2, VALUE_2);

        assertThat(root.getParent(), is(Optional.empty()));
        assertThat(root.getChildren(), empty());

        assertNodeProperty(root, KEY_1, VALUE_1);
        assertNodeProperty(root, KEY_2, null);
        assertEdgeProperty(root, KEY_1, null);
        assertEdgeProperty(root, KEY_2, VALUE_2);

        assertTreeProperties(root);
    }

    @Test
    public void testSimpleTwoNodeTree() {
        Node root = new Node();
        Node child = new Node();
        root.setNodeProperty(KEY_1, VALUE_1);

        child.setParent(root);
        child.setId("my-id");
        child.setEdgeProperty(KEY_2, VALUE_2);

        // check parent-child relationships
        assertThat(root.getParent(), is(Optional.empty()));
        assertThat(root.getChildren(), hasSize(1));

        assertThat(child.getParent(), is(Optional.of(root)));
        assertThat(child.getChildren(), empty());

        // check properties
        assertThat(root.getNodeProperties().keySet(), hasSize(1));
        assertThat(root.getEdgeProperties().keySet(), empty());
        assertThat(child.getNodeProperties().keySet(), empty());
        assertThat(child.getEdgeProperties().keySet(), hasSize(1));
        assertTreeProperties(root);
        assertTreeProperties(child);
    }

    @Test
    public void testSimpleTwoNodeTreeSwapped() {
        Node root = new Node();
        Node child = new Node();
        child.setParent(root);
        child.setId("my-id");

        // Now swap the roles
        root.setParent(child);

        // check parent-child relationships
        assertThat(child.getParent(), is(Optional.empty()));
        assertThat(child.getChildren(), hasSize(1));

        assertThat(root.getParent(), is(Optional.of(child)));
        assertThat(root.getChildren(), empty());

        // check properties have not changed
        root.setNodeProperty(KEY_1, VALUE_1);
        child.setEdgeProperty(KEY_2, VALUE_2);
        assertThat(root.getNodeProperties().keySet(), hasSize(1));
        assertThat(root.getEdgeProperties().keySet(), empty());
        assertThat(child.getNodeProperties().keySet(), empty());
        assertThat(child.getEdgeProperties().keySet(), hasSize(1));
        assertTreeProperties(root);
        assertTreeProperties(child);
    }

    @Test
    public void testStructureLinearThreeNodeSwapped() {
        Node level1 = new Node();
        Node level2 = new Node();
        Node level3 = new Node();

        level2.setParent(level1);
        level3.setParent(level2);

        // check parent-child relationships
        assertThat(level1.getParent(), is(Optional.empty()));
        assertThat(level1.getChildren(), containsInAnyOrder(level2));
        assertThat(level2.getParent(), is(Optional.of(level1)));
        assertThat(level2.getChildren(), containsInAnyOrder(level3));
        assertThat(level3.getParent(), is(Optional.of(level2)));
        assertThat(level3.getChildren(), empty());

        // Swap nodes
        level1.setParent(level3);

        // check parent-child relationships
        assertThat(level3.getParent(), is(Optional.empty()));
        assertThat(level3.getChildren(), containsInAnyOrder(level1));
        assertThat(level1.getParent(), is(Optional.of(level3)));
        assertThat(level1.getChildren(), containsInAnyOrder(level2));
        assertThat(level2.getParent(), is(Optional.of(level1)));
        assertThat(level2.getChildren(), empty());
    }

    @Test
    public void testStructureAThreeNodeSwapped() {
        Node level1 = new Node();
        Node level21 = new Node();
        Node level22 = new Node();
        Node level23 = new Node();

        level21.setParent(level1);
        level22.setParent(level1);
        level23.setParent(level1);

        // check parent-child relationships
        assertThat(level1.getParent(), is(Optional.empty()));
        assertThat(level1.getChildren(), containsInAnyOrder(level21, level22, level23));
        assertThat(level21.getParent(), is(Optional.of(level1)));
        assertThat(level21.getChildren(), empty());
        assertThat(level22.getParent(), is(Optional.of(level1)));
        assertThat(level22.getChildren(), empty());
        assertThat(level23.getParent(), is(Optional.of(level1)));
        assertThat(level23.getChildren(), empty());

        // Change parents
        level1.setParent(level22);

        // check parent-child relationships
        assertThat(level22.getParent(), is(Optional.empty()));
        assertThat(level22.getChildren(), containsInAnyOrder(level1));
        assertThat(level1.getParent(), is(Optional.of(level22)));
        assertThat(level1.getChildren(), containsInAnyOrder(level21, level23));
        assertThat(level21.getParent(), is(Optional.of(level1)));
        assertThat(level21.getChildren(), empty());
        assertThat(level23.getParent(), is(Optional.of(level1)));
        assertThat(level23.getChildren(), empty());

        // Change parents
        level21.setParent(level22);

        // check parent-child relationships
        assertThat(level22.getParent(), is(Optional.empty()));
        assertThat(level22.getChildren(), containsInAnyOrder(level1, level21));
        assertThat(level1.getParent(), is(Optional.of(level22)));
        assertThat(level1.getChildren(), containsInAnyOrder(level23));
        assertThat(level21.getParent(), is(Optional.of(level22)));
        assertThat(level21.getChildren(), empty());
        assertThat(level23.getParent(), is(Optional.of(level1)));
        assertThat(level23.getChildren(), empty());

        // Change parents
        level21.setParent(null);

        // check parent-child relationships
        assertThat(level22.getParent(), is(Optional.empty()));
        assertThat(level22.getChildren(), containsInAnyOrder(level1));
        assertThat(level1.getParent(), is(Optional.of(level22)));
        assertThat(level1.getChildren(), containsInAnyOrder(level23));
        assertThat(level21.getParent(), is(Optional.empty()));
        assertThat(level21.getChildren(), empty());
        assertThat(level23.getParent(), is(Optional.of(level1)));
        assertThat(level23.getChildren(), empty());
    }

    @Test
    public void testChangeId() {
        Node root = new Node();
        Node child1 = new Node();
        child1.setParent(root);
        child1.setId("my-id1");
        Node child2 = new Node();
        child2.setParent(root);
        child2.setId("my-id2");

        // check parent-child relationships
        assertThat(root.getChild("my-id1"), is(Optional.of(child1)));
        assertThat(root.getChild("my-id2"), is(Optional.of(child2)));

        child1.setId("my-id3");
        assertThat(root.getChild("my-id1"), is(Optional.empty()));
        assertThat(root.getChild("my-id3"), is(Optional.of(child1)));
    }

    @Test
    public void testRemoveChildDirectly() {
        Node root = new Node();
        Node child1 = new Node();
        child1.setParent(root);
        child1.setId("my-id1");
        Node child2 = new Node();
        child2.setParent(root);
        child2.setId("my-id2");

        // check parent-child relationships
        assertThat(root.getChild("my-id1"), is(Optional.of(child1)));
        assertThat(root.getChild("my-id2"), is(Optional.of(child2)));

        assertThat(root.removeChild(child1), is(Optional.of(child1)));

        assertThat(root.getChildren(), containsInAnyOrder(child2));
        assertThat(child1.getParent(), is(Optional.empty()));

        assertThat(root.removeChild(child1), is(Optional.empty()));

        // try to remove it once again
        assertThat(root.getChildren(), containsInAnyOrder(child2));
        assertThat(child1.getParent(), is(Optional.empty()));
    }

    @Test
    public void testRemoveChildViaPredicate() {
        Node root = new Node();
        Node child1 = new Node();
        child1.setParent(root);
        child1.setId("my-id1");
        Node child2 = new Node();
        child2.setParent(root);
        child2.setId("my-id2");
        Node child3 = new Node();
        child3.setParent(root);
        child3.setId("my-id3");

        // check removals
        assertThat(root.removeChild(node -> "my-id1".equals(node.getId())), is(1));

        assertThat(root.getChildren(), containsInAnyOrder(child2, child3));
        assertThat(child1.getParent(), is(Optional.empty()));
        assertThat(child2.getParent(), is(Optional.of(root)));
        assertThat(child3.getParent(), is(Optional.of(root)));

        assertThat(root.removeChild(node -> true), is(2));

        assertThat(root.getChildren(), empty());
        assertThat(child1.getParent(), is(Optional.empty()));
        assertThat(child2.getParent(), is(Optional.empty()));
        assertThat(child3.getParent(), is(Optional.empty()));
    }

    @Test
    public void testRemoveChild() {
        Node root = new Node();
        Node child1 = new Node();
        child1.setParent(root);
        child1.setId("my-id1");
        Node child2 = new Node();
        child2.setParent(root);
        child2.setId("my-id2");

        // check parent-child relationships
        assertThat(root.getChild("my-id1"), is(Optional.of(child1)));
        assertThat(root.getChild("my-id2"), is(Optional.of(child2)));

        root.removeChild(child1);

        assertThat(root.getChildren(), containsInAnyOrder(child2));
        assertThat(child1.getParent(), is(Optional.empty()));
    }

    @Test
    public void testDfs() {
        Node root = new Node("1");
        Node child11 = new Node(root, "1.1");
        Node child12 = new Node(root, "1.2");
        Node child111 = new Node(child11, "1.1.1");
        Node child112 = new Node(child11, "1.1.2");
        Node child121 = new Node(child12, "1.2.1");
        Node child122 = new Node(child12, "1.2.2");
        Node child123 = new Node(child12, "1.2.3");
        Node child1121 = new Node(child112, "1.1.2.1");

        List<Node> res = new LinkedList<>();
        assertThat(root.findFirstDfs(n -> {
            res.add(n);
            return false;
        }), is(Optional.empty()));
        assertThat(res, contains(root, child11, child111, child112, child1121, child12, child121, child122, child123));

        res.clear();
        assertThat(root.findFirstDfs(n -> {
            res.add(n);
            return n == child12;
        }), is(Optional.of(child12)));
        assertThat(res, contains(root, child11, child111, child112, child1121, child12));
    }

    @Test
    public void testDfsBottommost() {
        Node root = new Node("1");
        Node child11 = new Node(root, "1.1");
        Node child12 = new Node(root, "1.2");
        Node child13 = new Node(root, "1.3");
        Node child111 = new Node(child11, "1.1.1");
        Node child112 = new Node(child11, "1.1.2");
        Node child121 = new Node(child12, "1.2.1");
        Node child122 = new Node(child12, "1.2.2");
        Node child123 = new Node(child12, "1.2.3");
        Node child1121 = new Node(child112, "1.1.2.1");
        Node child131 = new Node(child13, "1.3.1");
        Node child132 = new Node(child13, "1.3.2");

        List<Node> res = new LinkedList<>();
        assertThat(root.findFirstBottommostDfs(n -> {
            res.add(n);
            return false;
        }), is(Optional.empty()));
        assertThat(res, contains(root, child11, child111, child112, child1121, child12, child121, child122, child123, child13, child131, child132));

        res.clear();
        assertThat(root.findFirstBottommostDfs(n -> {
            res.add(n);
            return n == child12;
        }), is(Optional.of(child12)));
        assertThat(res, contains(root, child11, child111, child112, child1121, child12, child121, child122, child123));

        res.clear();
        assertThat(root.findFirstBottommostDfs(n -> {
            res.add(n);
            return n.getId().startsWith("1.1.2");
        }), is(Optional.of(child1121)));
        assertThat(res, contains(root, child11, child111, child112, child1121));
    }

    @Test
    public void testBfs() {
        Node root = new Node("1");
        Node child11 = new Node(root, "1.1");
        Node child12 = new Node(root, "1.2");
        Node child111 = new Node(child11, "1.1.1");
        Node child112 = new Node(child11, "1.1.2");
        Node child121 = new Node(child12, "1.2.1");
        Node child122 = new Node(child12, "1.2.2");
        Node child123 = new Node(child12, "1.2.3");
        Node child1121 = new Node(child112, "1.1.2.1");

        List<Node> res = new LinkedList<>();
        assertThat(root.findFirstBfs(n -> {
            res.add(n);
            return false;
        }), is(Optional.empty()));
        assertThat(res, contains(root, child11, child12, child111, child112, child121, child122, child123, child1121));

        res.clear();
        assertThat(root.findFirstBfs(n -> {
            res.add(n);
            return n == child12;
        }), is(Optional.of(child12)));
        assertThat(res, contains(root, child11, child12));
    }

    @Test
    public void testWalkBfs() {
        Node root = new Node("1");
        Node child11 = new Node(root, "1.1");
        Node child12 = new Node(root, "1.2");
        Node child111 = new Node(child11, "1.1.1");
        Node child112 = new Node(child11, "1.1.2");
        Node child121 = new Node(child12, "1.2.1");
        Node child122 = new Node(child12, "1.2.2");
        Node child123 = new Node(child12, "1.2.3");
        Node child1121 = new Node(child112, "1.1.2.1");

        List<Node> res = new LinkedList<>();
        root.walkBfs(res::add);
        assertThat(res, contains(root, child11, child12, child111, child112, child121, child122, child123, child1121));
    }

    @Test
    public void testWalkDfs() {
        Node root = new Node("1");
        Node child11 = new Node(root, "1.1");
        Node child12 = new Node(root, "1.2");
        Node child111 = new Node(child11, "1.1.1");
        Node child112 = new Node(child11, "1.1.2");
        Node child121 = new Node(child12, "1.2.1");
        Node child122 = new Node(child12, "1.2.2");
        Node child123 = new Node(child12, "1.2.3");
        Node child1121 = new Node(child112, "1.1.2.1");

        List<Node> uponEntry = new LinkedList<>();
        List<Node> afterChildren = new LinkedList<>();
        root.walkDfs(uponEntry::add, afterChildren::add);
        assertThat(uponEntry, contains(root, child11, child111, child112, child1121, child12, child121, child122, child123));
        assertThat(afterChildren, contains(child111, child1121, child112, child11, child121, child122, child123, child12, root));
    }

    @Test
    public void testForEachParent() {
        Node root = new Node("1");
        Node child11 = new Node(root, "1.1");
        Node child12 = new Node(root, "1.2");
        Node child111 = new Node(child11, "1.1.1");
        Node child112 = new Node(child11, "1.1.2");
        Node child121 = new Node(child12, "1.2.1");
        Node child122 = new Node(child12, "1.2.2");
        Node child123 = new Node(child12, "1.2.3");
        Node child1121 = new Node(child112, "1.1.2.1");

        List<Node> res = new LinkedList<>();
        res.clear();
        root.forEachParent(res::add);
        assertThat(res, empty());

        res.clear();
        child1121.forEachParent(res::add);
        assertThat(res, contains(child112, child11, root));

        res.clear();
        child123.forEachParent(res::add);
        assertThat(res, contains(child12, root));
    }

    @Test
    public void testPathToRoot() {
        Node root = new Node("1");
        Node child11 = new Node(root, "1.1");
        Node child12 = new Node(root, "1.2");
        Node child111 = new Node(child11, "1.1.1");
        Node child112 = new Node(child11, "1.1.2");
        Node child121 = new Node(child12, "1.2.1");
        Node child122 = new Node(child12, "1.2.2");
        Node child123 = new Node(child12, "1.2.3");
        Node child1121 = new Node(child112, "1.1.2.1");

        assertThat(child1121.getPathToRoot(PathOrientation.TOP_FIRST), contains(root, child11, child112, child1121));
        assertThat(child123.getPathToRoot(PathOrientation.TOP_FIRST), contains(root, child12, child123));
        assertThat(root.getPathToRoot(PathOrientation.TOP_FIRST), contains(root));

        assertThat(child1121.getPathToRoot(PathOrientation.BOTTOM_FIRST), contains(child1121, child112, child11, root));
        assertThat(child123.getPathToRoot(PathOrientation.BOTTOM_FIRST), contains(child123, child12, root));
        assertThat(root.getPathToRoot(PathOrientation.BOTTOM_FIRST), contains(root));
    }

    @Test
    public void testToStringStackOverflow() {
        Node n = new Node("1");
        n.setNodeProperty("prop", n);
        assertThat(n.toString().length(), lessThan(255));
    }

    private void assertTreeProperties(Node node) {
        assertThat(node.getTreeProperty(KEY_1, String.class), notNullValue());
        assertThat(node.getTreeProperty(KEY_1, Date.class), notNullValue());

        assertThat(node.getTreeProperty(KEY_1, String.class), is(Optional.of(VALUE_1)));
        assertThat(node.getTreeProperty(KEY_1, Date.class), is(Optional.empty()));

        assertThat(node.getTreeProperty(KEY_2, String.class), is(Optional.empty()));
        assertThat(node.getTreeProperty(KEY_2, Date.class), is(Optional.of(VALUE_2)));

        assertThat(node.getTreeProperties().size(), is(2));

        treeProperties.put(KEY_3, VALUE_3);
        assertThat(node.getTreeProperties().size(), is(3));
        assertThat(node.getTreeProperty(KEY_3, String.class), is(Optional.empty()));
        assertThat(node.getTreeProperty(KEY_3, Integer.class), is(Optional.of(VALUE_3)));

        treeProperties.remove(KEY_3);
        assertThat(node.getTreeProperties().size(), is(2));
        assertThat(node.getTreeProperties(), not(hasKey(KEY_3)));
    }

    private void assertNodeProperty(Node node, String key, Object value) {
        if (value != null) {
            assertThat(node.getNodeProperty(key, value.getClass()), is(Optional.of(value)));
            assertThat(node.getNodeProperty(key, Object.class), is(Optional.of(value)));
            assertThat(node.getNodeProperty(key, Throwable.class), is(Optional.empty()));
        } else {
            assertThat(node.getNodeProperty(key, Object.class), is(Optional.empty()));
        }
    }

    private void assertEdgeProperty(Node node, String key, Object value) {
        if (value != null) {
            assertThat(node.getEdgeProperty(key, value.getClass()), is(Optional.of(value)));
            assertThat(node.getEdgeProperty(key, Object.class), is(Optional.of(value)));
            assertThat(node.getEdgeProperty(key, Throwable.class), is(Optional.empty()));
        } else {
            assertThat(node.getEdgeProperty(key, Object.class), is(Optional.empty()));
        }
    }
}
