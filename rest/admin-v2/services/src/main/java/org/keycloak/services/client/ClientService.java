package org.keycloak.services.client;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.PatchType;
import org.keycloak.services.Service;
import org.keycloak.services.ServiceException;

import static org.keycloak.utils.StringUtil.isBlank;

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

    class ClientSortAndSliceOptions {
        private final ClientSortField sortField;
        private final boolean ascending;

        private ClientSortAndSliceOptions(ClientSortField sortField, boolean ascending) {
            this.sortField = sortField;
            this.ascending = ascending;
        }

        public static ClientSortAndSliceOptions fromQuery(String sortBy, String sortOrder) {
            ClientSortField field = isBlank(sortBy)
                    ? ClientSortField.defaultField()
                    : parseSortBy(sortBy);
            return new ClientSortAndSliceOptions(field, parseSortOrder(sortOrder));
        }

        private static ClientSortField parseSortBy(String sortBy) {
            if (sortBy.contains(",")) {
                throw new ServiceException("Only a single sort field is supported", Response.Status.BAD_REQUEST);
            }
            String field = sortBy.trim();
            ClientSortField.validateApiName(field).ifPresent(msg -> {
                throw new ServiceException(msg, Response.Status.BAD_REQUEST);
            });
            return ClientSortField.fromApiName(field).orElseThrow();
        }

        private static boolean parseSortOrder(String sortOrder) {
            if (isBlank(sortOrder) || "asc".equalsIgnoreCase(sortOrder)) {
                return true;
            }
            if ("desc".equalsIgnoreCase(sortOrder)) {
                return false;
            }
            throw new ServiceException("sortOrder must be asc or desc", Response.Status.BAD_REQUEST);
        }

        public ClientSortField getSortField() {
            return sortField;
        }

        public boolean isAscending() {
            return ascending;
        }

        // offset / limit — #48289
    }

    record CreateOrUpdateResult(BaseClientRepresentation representation, boolean created) {}

    Optional<BaseClientRepresentation> getClient(RealmModel realm, String clientId) throws ServiceException;

    Stream<BaseClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions, ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions);

    Stream<BaseClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions);

    void deleteClient(RealmModel realm, String clientId) throws ServiceException;

    CreateOrUpdateResult createOrUpdateClient(RealmModel realm, String clientId, BaseClientRepresentation client) throws ServiceException;

    BaseClientRepresentation createClient(RealmModel realm, BaseClientRepresentation client) throws ServiceException;

    BaseClientRepresentation patchClient(RealmModel realm, String clientId, PatchType patchType, InputStream patch) throws ServiceException;
}
