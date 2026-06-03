package org.keycloak.services.client;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.api.ClientSortField;
import org.keycloak.admin.api.ListOptions;
import org.keycloak.admin.api.SortOrder;
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
        private final List<ClientSortField> sortFields;
        private final boolean ascending;

        private ClientSortAndSliceOptions(List<ClientSortField> sortFields, boolean ascending) {
            this.sortFields = List.copyOf(sortFields);
            this.ascending = ascending;
        }

        public static ClientSortAndSliceOptions fromQuery(ListOptions listOptions) {
            List<ClientSortField> fields = isBlank(listOptions.getSortBy())
                    ? List.of(ClientSortField.defaultField())
                    : parseSortBy(listOptions.getSortBy());
            return new ClientSortAndSliceOptions(fields, parseSortOrder(listOptions.getSortOrder()));
        }

        private static List<ClientSortField> parseSortBy(String sortBy) {
            List<ClientSortField> fields = Arrays.stream(sortBy.split(","))
                    .map(String::trim)
                    .filter(field -> !field.isEmpty())
                    .map(ClientSortAndSliceOptions::parseSortField)
                    .toList();
            if (fields.isEmpty()) {
                throw new ServiceException("sortBy must specify at least one field", Response.Status.BAD_REQUEST);
            }
            return fields;
        }

        private static ClientSortField parseSortField(String field) {
            ClientSortField.validateApiName(field).ifPresent(msg -> {
                throw new ServiceException(msg, Response.Status.BAD_REQUEST);
            });
            return ClientSortField.fromApiName(field).orElseThrow();
        }

        private static boolean parseSortOrder(String sortOrder) {
            return SortOrder.fromQueryValue(sortOrder)
                    .orElseThrow(() -> new ServiceException("sortOrder must be asc or desc", Response.Status.BAD_REQUEST))
                    .isAscending();
        }

        public Comparator<BaseClientRepresentation> getSortComparator() {
            return sortFields.stream()
                    .map(field -> field.comparator(ascending))
                    .reduce(Comparator::thenComparing)
                    .orElseThrow();
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
