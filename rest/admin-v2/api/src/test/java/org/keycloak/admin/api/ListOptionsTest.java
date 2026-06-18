package org.keycloak.admin.api;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListOptionsTest {

    @Test
    void testGetFieldsEmpty() {
        ListOptions options = new ListOptions();
        options.setFields(Set.of());
        assertEquals("", options.fields);
        assertTrue(options.getFields().isEmpty());
    }
    
    @Test
    void testGetFields() {
        ListOptions options = new ListOptions();
        options.fields = "a,b";
        assertTrue(options.getFields().equals(Set.of("a", "b")));
    }

}
