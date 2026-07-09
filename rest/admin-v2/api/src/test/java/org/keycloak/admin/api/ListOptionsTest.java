package org.keycloak.admin.api;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void testGetSortEmpty() {
        ListOptions options = new ListOptions();
        options.setSort(List.of());
        assertEquals("", options.sort);
        assertEquals(List.of(), options.getSort());
    }

    @Test
    void testGetSort() {
        ListOptions options = new ListOptions();
        options.sort = "displayName|desc,clientId";
        assertEquals(List.of(SortOption.of(ClientField.DISPLAY_NAME, SortOrder.DESC), SortOption.of(ClientField.CLIENT_ID)),
                options.getSort());
    }

    @Test
    void getSortCachesParsedResult() {
        ListOptions options = new ListOptions();
        options.sort = "displayName|desc,clientId";

        List<SortOption> first = options.getSort();
        List<SortOption> second = options.getSort();

        assertEquals(List.of(SortOption.of(ClientField.DISPLAY_NAME, SortOrder.DESC), SortOption.of(ClientField.CLIENT_ID)),
                first);
        assertSame(first, second);
    }

    @Test
    void testSetSort() {
        ListOptions options = new ListOptions();
        options.setSort(List.of(SortOption.of(ClientField.CLIENT_ID, SortOrder.DESC)));
        assertEquals("clientId|desc", options.sort);
    }

    @Test
    void testSortTime() {
        ListOptions options = new ListOptions();
        options.setSort(List.of(SortOption.of(ClientField.CREATED_TIMESTAMP, SortOrder.DESC)));
        assertEquals("createdTimestamp|desc", options.sort);
    }

    @Test
    void invalidSortFieldThrowsBadRequest() {
        ListOptions options = new ListOptions();
        options.sort = "unknown";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, options::getSort);
        assertEquals("unknown is not a sortable field", exception.getMessage());
    }

    @Test
    void invalidSortDirectionThrowsBadRequest() {
        ListOptions options = new ListOptions();
        options.sort = "clientId|what";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, options::getSort);
        assertEquals("sort direction must be asc or desc", exception.getMessage());
    }

}
