package org.keycloak.services.client;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.api.ListOptions;
import org.keycloak.admin.api.SortOption;
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

    class ClientSortAndSliceOptions {
        private List<SortOption<ClientField>> sortOptions;
        private int offset;
        private int limit;

        private ClientSortAndSliceOptions(List<SortOption<ClientField>> sortOptions, int offset, int limit) {
            this.offset = offset;
            this.limit = limit;
            this.sortOptions = List.copyOf(sortOptions);
        }

        public int limit() {
            return this.limit;
        }

        public int offset() {
            return this.offset;
        }

        public static ClientSortAndSliceOptions fromQuery(ListOptions listOptions) {
            List<SortOption<ClientField>> options;
            int normalizedOffset;
            int normalizedLimit;
            try {
                var sort = listOptions.getSort(ClientField::fromApiName);
                options = sort == null || sort.isEmpty()
                        ? List.of(SortOption.of(ClientField.defaultField()))
                        : sort;

                Integer offset = listOptions.getOffset();
                Integer limit = listOptions.getLimit();
                if (offset != null && offset < 0) {
                    throw new ServiceException("offset must be greater than or equal to 0", Response.Status.BAD_REQUEST);
                }
                if (limit != null && limit < 1) {
                    throw new ServiceException("limit must be greater than or equal to 1", Response.Status.BAD_REQUEST);
                }
                normalizedOffset = offset != null ? offset : 0;
                normalizedLimit = limit != null ? limit : Constants.DEFAULT_MAX_RESULTS;
            } catch (IllegalArgumentException e) {
                throw new ServiceException(e.getMessage(), Response.Status.BAD_REQUEST);
            }
            return new ClientSortAndSliceOptions(options, normalizedOffset, normalizedLimit);
        }

        public Comparator<BaseClientRepresentation> getSortComparator() {
            return sortOptions.stream()
                    .map(option -> option.field().comparator(option.isAscending()))
                    .reduce(Comparator::thenComparing)
                    .orElseThrow();
        }
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
