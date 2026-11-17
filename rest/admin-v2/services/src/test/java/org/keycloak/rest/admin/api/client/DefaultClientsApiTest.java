package org.keycloak.rest.admin.api.client;

import java.util.List;

import org.keycloak.scim.resource.spi.SortField;
import org.keycloak.scim.resource.spi.SortField.SortOrder;
import org.keycloak.services.ServiceException;
import org.keycloak.services.error.ProblemType;

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
        ServiceException exception = assertThrows(ServiceException.class, () -> DefaultClientsApi.getSortFields("|desc"));
        assertEquals(ProblemType.VALIDATION_FAILED, exception.getType());
        assertEquals("sort must specify at least one field", exception.getMessage());
    }

    @Test
    void invalidSortDirectionThrowsBadRequest() {
        ServiceException exception = assertThrows(ServiceException.class, () -> DefaultClientsApi.getSortFields("foo|what"));
        assertEquals(ProblemType.VALIDATION_FAILED, exception.getType());
        assertEquals("sort direction must be asc or desc", exception.getMessage());
    }

}
