package org.keycloak.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

// A custom RoleProvider that does NOT override getCompositeRolesStream must still resolve composite
// children correctly through the interface default implementation (de-duplicated, per parent).
public class RoleProviderCompositeDefaultTest {

    @Test
    public void defaultGetCompositeRolesStreamResolvesAndDeduplicates() {
        // root -> a, b ; a -> c ; b -> c (diamond, c shared) ; c is a leaf
        FakeRole c = new FakeRole("c");
        FakeRole a = new FakeRole("a", c);
        FakeRole b = new FakeRole("b", c);
        FakeRole root = new FakeRole("root", a, b);
        FakeRoleProvider provider = new FakeRoleProvider(root, a, b, c);

        Assert.assertEquals(Set.of("a", "b"), childIds(provider, "root"));
        // c is shared by a and b but must appear once
        Assert.assertEquals(Set.of("c"), childIds(provider, "a", "b"));
        Assert.assertEquals(Set.of(), childIds(provider, "c"));
        Assert.assertEquals(Set.of(), provider.getCompositeRolesStream(null, Set.of()).collect(Collectors.toSet()));
    }

    private static Set<String> childIds(FakeRoleProvider provider, String... parentIds) {
        return provider.getCompositeRolesStream(null, Set.of(parentIds))
                .map(RoleModel::getId).collect(Collectors.toSet());
    }

    // RoleProvider stub backed by an in-memory role map; getCompositeRolesStream is left to the default.
    private static final class FakeRoleProvider implements RoleProvider {

        private final Map<String, RoleModel> byId = new HashMap<>();

        FakeRoleProvider(RoleModel... roles) {
            for (RoleModel r : roles) byId.put(r.getId(), r);
        }

        @Override
        public RoleModel getRoleById(RealmModel realm, String id) {
            return byId.get(id);
        }

        // --- unused RoleProvider / RoleLookupProvider surface --------------------------------

        @Override public RoleModel addRealmRole(RealmModel realm, String id, String name) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public boolean removeRole(RoleModel role) { throw new UnsupportedOperationException(); }
        @Override public void removeRoles(RealmModel realm) { throw new UnsupportedOperationException(); }
        @Override public RoleModel addClientRole(ClientModel client, String id, String name) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public void removeRoles(ClientModel client) { throw new UnsupportedOperationException(); }
        @Override public RoleModel getRealmRole(RealmModel realm, String name) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public RoleModel getClientRole(ClientModel client, String name) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, String search, Stream<String> excludedIds, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public void close() { }
    }

    // RoleModel stub; identity by id, composites held in memory.
    private static final class FakeRole implements RoleModel {

        private final String id;
        private final List<RoleModel> composites = new ArrayList<>();

        FakeRole(String id, RoleModel... children) {
            this.id = id;
            this.composites.addAll(Arrays.asList(children));
        }

        @Override public String getId() { return id; }
        @Override public boolean isComposite() { return !composites.isEmpty(); }
        @Override public Stream<RoleModel> getCompositesStream() { return new ArrayList<>(composites).stream(); }

        @Override public boolean equals(Object o) { return o instanceof RoleModel && ((RoleModel) o).getId().equals(id); }
        @Override public int hashCode() { return id.hashCode(); }

        // --- unused RoleModel surface --------------------------------------------------------

        @Override public String getName() { return id; }
        @Override public String getDescription() { throw new UnsupportedOperationException(); }
        @Override public void setDescription(String description) { throw new UnsupportedOperationException(); }
        @Override public void setName(String name) { throw new UnsupportedOperationException(); }
        @Override public void addCompositeRole(RoleModel role) { throw new UnsupportedOperationException(); }
        @Override public void removeCompositeRole(RoleModel role) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public boolean isClientRole() { throw new UnsupportedOperationException(); }
        @Override public String getContainerId() { throw new UnsupportedOperationException(); }
        @Override public RoleContainerModel getContainer() { throw new UnsupportedOperationException(); }
        @Override public boolean hasRole(RoleModel role) { throw new UnsupportedOperationException(); }
        @Override public void setSingleAttribute(String name, String value) { throw new UnsupportedOperationException(); }
        @Override public void setAttribute(String name, List<String> values) { throw new UnsupportedOperationException(); }
        @Override public void removeAttribute(String name) { throw new UnsupportedOperationException(); }
        @Override public Stream<String> getAttributeStream(String name) { throw new UnsupportedOperationException(); }
        @Override public Map<String, List<String>> getAttributes() { throw new UnsupportedOperationException(); }
    }
}
