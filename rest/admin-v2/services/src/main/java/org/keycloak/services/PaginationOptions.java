package org.keycloak.services;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.Constants;
import org.keycloak.services.client.ClientService.ClientSortAndSliceOptions;

public final class PaginationOptions {

    public static final int MAX_LIMIT = Constants.DEFAULT_MAX_RESULTS;

    private PaginationOptions() {
    }

    public static ClientSortAndSliceOptions normalize(Integer offset, Integer limit) throws ServiceException {
        if (offset != null && offset < 0) {
            throw new ServiceException("offset must be greater than or equal to 0", Response.Status.BAD_REQUEST);
        }
        if (limit != null && limit < 0) {
            throw new ServiceException("limit must be greater than or equal to 0", Response.Status.BAD_REQUEST);
        }
        int normalizedOffset = offset != null ? offset : 0;
        int normalizedLimit = limit != null ? limit : MAX_LIMIT;
        return new ClientSortAndSliceOptions(normalizedOffset, normalizedLimit);
    }
}
