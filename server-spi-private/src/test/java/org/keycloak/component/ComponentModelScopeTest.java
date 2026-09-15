package org.keycloak.component;

import java.util.Set;

import org.keycloak.Config.AbstractScope;
import org.keycloak.Config.Scope;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ComponentModelScopeTest {

    @Test public void testGetters() {
        Scope scope = new AbstractScope() {

            @Override
            public Scope scope(String... scope) {
                return null;
            }

            @Override
            public Scope root() {
                return null;
            }

            @Override
            public Set<String> getPropertyNames() {
                return null;
            }

            @Override
            public String get(String key) {
                if (key.equals("base")) {
                    return "x, y";
                }
                if (key.equals("int")) {
                    return "1";
                }
                return null;
            }
        };

        ComponentModel model = new ComponentModel();
        model.put("component", "a");
        model.put("string", "abc");

        ComponentModelScope componentModelScope = new ComponentModelScope(scope, model);

        assertArrayEquals(new String[] {"x", "y"}, componentModelScope.getArray("base"));
        assertArrayEquals(new String[] {"a"}, componentModelScope.getArray("component"));

        assertEquals(Integer.valueOf(1), componentModelScope.getInt("int"));
        assertEquals(Long.valueOf(1), componentModelScope.getLong("int"));
        assertEquals("abc", componentModelScope.get("string", "default"));
        assertEquals(null, componentModelScope.get("doesn't exist"));
    }

}
