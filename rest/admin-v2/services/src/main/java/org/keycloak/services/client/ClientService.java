package org.keycloak.services.client;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.Constants;
import org.keycloak.admin.api.ClientField;
import org.keycloak.admin.api.ListOptions;
import org.keycloak.admin.api.SortOption;
import org.keycloak.admin.api.SortOrder;
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
        private final List<SortOption> sortOptions;

        private ClientSortAndSliceOptions(List<SortOption> sortOptions) {
            this.sortOptions = List.copyOf(sortOptions);
        }

        public static ClientSortAndSliceOptions fromQuery(ListOptions listOptions) {
            List<SortOption> options = listOptions.getSort() == null || listOptions.getSort().isEmpty()
                    ? List.of(SortOption.of(ClientField.defaultField()))
                    : parseSort(listOptions.getSort());
            return new ClientSortAndSliceOptions(options);
        }

        private static List<SortOption> parseSort(String sort) {
            List<SortOption> options = Arrays.stream(sort.split(","))
                    .map(String::trim)
                    .filter(segment -> !segment.isEmpty())
                    .map(ClientSortAndSliceOptions::parseSortSegment)
                    .collect(Collectors.toList());
            if (options.isEmpty()) {
                throw new ServiceException("sort must specify at least one field", Response.Status.BAD_REQUEST);
            }
            return options;
        }

        private static SortOption parseSortSegment(String segment) {
            String[] parts = segment.split("\\|", 2);
            String fieldName = parts[0].trim();
            if (fieldName.isEmpty()) {
                throw new ServiceException("sort must specify at least one field", Response.Status.BAD_REQUEST);
            }
            ClientField field = ClientField.fromApiName(fieldName).orElseThrow(() ->
                    new ServiceException(String.format("%s is not a sortable field", fieldName), Response.Status.BAD_REQUEST));
            SortOrder order = parts.length == 1 ? SortOrder.ASC : parseSortOrder(parts[1].trim());
            return SortOption.of(field, order);
        }

        private static SortOrder parseSortOrder(String value) {
            if (value.isEmpty()) {
                return SortOrder.ASC;
            }
            for (SortOrder order : SortOrder.values()) {
                if (order.name().equalsIgnoreCase(value)) {
                    return order;
                }
            }
            throw new ServiceException("sort direction must be asc or desc", Response.Status.BAD_REQUEST);
        }

        public Comparator<BaseClientRepresentation> getSortComparator() {
            return sortOptions.stream()
                    .map(option -> option.field().comparator(option.isAscending()))
                    .reduce(Comparator::thenComparing)
                    .orElseThrow();
        }

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
