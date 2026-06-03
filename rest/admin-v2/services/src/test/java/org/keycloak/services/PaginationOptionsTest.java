package org.keycloak.services;

import org.keycloak.services.client.ClientService.ClientSortAndSliceOptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationOptionsTest {

    @Test
    void normalizeDefaults() throws ServiceException {
        ClientSortAndSliceOptions options = PaginationOptions.normalize(null, null);
        assertEquals(0, options.offset());
        assertEquals(100, options.limit());
    }

    @Test
    void normalizeCapsLimit() throws ServiceException {
        ClientSortAndSliceOptions options = PaginationOptions.normalize(10, 200);
        assertEquals(10, options.offset());
        assertEquals(200, options.limit());
    }

    @Test
    void normalizeRejectsNegativeOffset() {
        assertThrows(ServiceException.class, () -> PaginationOptions.normalize(-1, 10));
    }

    @Test
    void normalizeRejectsNegativeLimit() {
        assertThrows(ServiceException.class, () -> PaginationOptions.normalize(0, -1));
    }
}
