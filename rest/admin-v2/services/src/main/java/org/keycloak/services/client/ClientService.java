package org.keycloak.services.client;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.PatchType;
import org.keycloak.services.Service;
import org.keycloak.services.ServiceException;

public interface ClientService extends Service {

    record ClientSearchOptions(String query) {}

    class ClientProjectionOptions {
        private final LinkedHashSet<String> fields = new LinkedHashSet<>();

        public ClientProjectionOptions(Set<String> fields) {
            if (fields != null) {
                this.fields.addAll(fields);
            }
        }
        
        public Set<String> getFields() {
            return Collections.unmodifiableSet(fields);
        }
    }

    record ClientSortAndSliceOptions(int offset, int limit) {}

    record CreateOrUpdateResult(BaseClientRepresentation representation, boolean created) {}

    Optional<BaseClientRepresentation> getClient(RealmModel realm, String clientId) throws ServiceException;

    Stream<BaseClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions, ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions);

    Stream<BaseClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions);

    void deleteClient(RealmModel realm, String clientId) throws ServiceException;

    CreateOrUpdateResult createOrUpdateClient(RealmModel realm, String clientId, BaseClientRepresentation client) throws ServiceException;

    BaseClientRepresentation createClient(RealmModel realm, BaseClientRepresentation client) throws ServiceException;

    BaseClientRepresentation patchClient(RealmModel realm, String clientId, PatchType patchType, InputStream patch) throws ServiceException;

    public static ClientSortAndSliceOptions normalizePagination(Integer offset, Integer limit) throws ServiceException {
        if (offset != null && offset < 0) {
            throw new ServiceException("offset must be greater than or equal to 0", Response.Status.BAD_REQUEST);
        }
        if (limit != null && limit < 1) {
            throw new ServiceException("limit must be greater than or equal to 1", Response.Status.BAD_REQUEST);
        }
        int normalizedOffset = offset != null ? offset : 0;
        int normalizedLimit = limit != null ? limit : Constants.DEFAULT_MAX_RESULTS;
        return new ClientSortAndSliceOptions(normalizedOffset, normalizedLimit);
    }
}
