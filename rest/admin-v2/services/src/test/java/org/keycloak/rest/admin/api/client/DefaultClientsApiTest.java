package org.keycloak.rest.admin.api.client;

import java.util.List;

import org.keycloak.scim.resource.spi.SortField;
import org.keycloak.scim.resource.spi.SortField.SortOrder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultClientsApiTest {
    
    @Test
    void testGetSortFields() {
        assertEquals(List.of(new SortField("bar", SortOrder.DESC), new SortField("foo", SortOrder.ASC)),
                DefaultClientsApi.getSortFields("bar|desc,foo"));
    }

    @Test
    void invalidSortFormat() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> DefaultClientsApi.getSortFields("|desc"));
        assertEquals("sort must specify at least one field", exception.getMessage());
    }

    @Test
    void invalidSortDirectionThrowsBadRequest() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> DefaultClientsApi.getSortFields("foo|what"));
        assertEquals("sort direction must be asc or desc", exception.getMessage());
    }

}
