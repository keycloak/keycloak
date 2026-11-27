package org.keycloak.admin.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListOptionsTest {

    @Test
    void testGetFieldsEmpty() {
        ListOptions options = new ListOptions();
        options.setFields(Collections.emptySet());
        assertEquals("", options.fields);
        assertEquals(Collections.emptySet(), options.getFields());
    }
    
    @Test
    void testGetFields() {
        ListOptions options = new ListOptions();
        options.fields = "a,b";
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), options.getFields());
    }

    @Test
    void testAddSortFields() {
        ListOptions options = new ListOptions();
        options.addSortField("field");
        assertEquals("field", options.getSort());
        options.addSortField("field2", false);
        assertEquals("field,field2|DESC", options.getSort());
    }

}
