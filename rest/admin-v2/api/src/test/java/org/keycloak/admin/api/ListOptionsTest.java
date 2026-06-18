package org.keycloak.admin.api;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListOptionsTest {

    @Test
    void testGetFieldsEmpty() {
        ListOptions options = new ListOptions();
        options.setFields(Set.of());
        assertEquals("", options.fields);
        assertEquals(Set.of(), options.getFields());
    }
    
    @Test
    void testGetFields() {
        ListOptions options = new ListOptions();
        options.fields = "a,b";
        assertEquals(Set.of("a", "b"), options.getFields());
    }

}
