package org.keycloak.models.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

public class RoleUtilsCompositeExpansionTest {

    @Test
    public void expandsFullTreeWithoutCallingIsComposite() {
        // root -> a, b ; a -> c ; b -> c (diamond, c shared) ; c is a leaf
        FakeRole c = new FakeRole("c");
        FakeRole a = new FakeRole("a", c);
        FakeRole b = new FakeRole("b", c);
        FakeRole root = new FakeRole("root", a, b);

        Set<RoleModel> expanded = RoleUtils.expandCompositeRoles(Set.of(root));

        Assert.assertEquals(Set.of(root, a, b, c), expanded);

        // The optimization: isComposite() is never used to drive the expansion.
        for (FakeRole r : Arrays.asList(root, a, b, c)) {
            Assert.assertEquals("isComposite() must not be called for " + r.id, 0, r.isCompositeCalls);
        }
        // Each reachable role is resolved exactly once thanks to the visited set.
        Assert.assertEquals(1, root.getCompositesCalls);
        Assert.assertEquals(1, a.getCompositesCalls);
        Assert.assertEquals(1, b.getCompositesCalls);
        Assert.assertEquals(1, c.getCompositesCalls);
    }

    @Test
    public void leafRoleExpandsToItself() {
        FakeRole leaf = new FakeRole("leaf");

        Assert.assertEquals(Set.of(leaf), RoleUtils.expandCompositeRoles(Set.of(leaf)));
        Assert.assertEquals(0, leaf.isCompositeCalls);
    }

    @Test
    public void cyclicCompositesTerminate() {
        // root -> a -> root (cycle)
        FakeRole a = new FakeRole("a");
        FakeRole root = new FakeRole("root", a);
        a.composites.add(root);

        Set<RoleModel> expanded = RoleUtils.expandCompositeRoles(Set.of(root));

        Assert.assertEquals(Set.of(root, a), expanded);
    }

    // RoleModel stub recording isComposite()/getCompositesStream() calls; identity by id.
    private static final class FakeRole implements RoleModel {

        final String id;
        final List<RoleModel> composites = new ArrayList<>();
        int isCompositeCalls = 0;
        int getCompositesCalls = 0;

        FakeRole(String id, RoleModel... children) {
            this.id = id;
            this.composites.addAll(Arrays.asList(children));
        }

        @Override
        public boolean isComposite() {
            isCompositeCalls++;
            return !composites.isEmpty();
        }

        @Override
        public Stream<RoleModel> getCompositesStream() {
            getCompositesCalls++;
            return new ArrayList<>(composites).stream();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RoleModel && ((RoleModel) o).getId().equals(id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        // --- unused RoleModel surface ---------------------------------------------------------

        @Override
        public String getName() {
            return id;
        }

        @Override
        public String getDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDescription(String description) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setName(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addCompositeRole(RoleModel role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeCompositeRole(RoleModel role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isClientRole() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContainerId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RoleContainerModel getContainer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasRole(RoleModel role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSingleAttribute(String name, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAttribute(String name, List<String> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<String> getAttributeStream(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, List<String>> getAttributes() {
            throw new UnsupportedOperationException();
        }
    }
}
