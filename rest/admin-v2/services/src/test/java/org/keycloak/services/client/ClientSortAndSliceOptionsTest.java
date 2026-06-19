package org.keycloak.services.client;

import java.util.Comparator;
import java.util.List;

import org.keycloak.admin.api.ClientField;
import org.keycloak.admin.api.ListOptions;
import org.keycloak.admin.api.SortOption;
import org.keycloak.admin.api.SortOrder;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService.ClientSortAndSliceOptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientSortAndSliceOptionsTest {

    @Test
    void defaultSortUsesClientIdAscending() {
        Comparator<BaseClientRepresentation> comparator = ClientSortAndSliceOptions.fromQuery(new ListOptions()).getSortComparator();

        BaseClientRepresentation a = client("a");
        BaseClientRepresentation b = client("b");

        assertEquals(-1, comparator.compare(a, b));
        assertEquals(1, comparator.compare(b, a));
    }

    @Test
    void parseSortWithPerFieldDirections() {
        ListOptions options = new ListOptions();
        options.setSort("displayName|desc,clientId|asc");

        Comparator<BaseClientRepresentation> comparator = ClientSortAndSliceOptions.fromQuery(options).getSortComparator();

        BaseClientRepresentation first = client("sort-a", "A");
        BaseClientRepresentation second = client("sort-b", "A");
        BaseClientRepresentation third = client("sort-c", "B");

        assertEquals(-1, comparator.compare(third, first));
        assertEquals(-1, comparator.compare(first, second));
    }

    @Test
    void parseSortUsesDefaultDirectionWhenOmitted() {
        ListOptions options = new ListOptions();
        options.setSort(List.of(SortOption.of(ClientField.DISPLAY_NAME), SortOption.of(ClientField.CLIENT_ID, SortOrder.DESC)));

        Comparator<BaseClientRepresentation> comparator = ClientSortAndSliceOptions.fromQuery(options).getSortComparator();

        BaseClientRepresentation first = client("sort-a", "A");
        BaseClientRepresentation second = client("sort-b", "A");
        BaseClientRepresentation third = client("sort-c", "B");

        assertEquals(1, comparator.compare(first, second));
        assertEquals(1, comparator.compare(third, first));
    }

    @Test
    void invalidSortFieldThrowsBadRequest() {
        ListOptions options = new ListOptions();
        options.setSort("unknown");

        ServiceException exception = assertThrows(ServiceException.class, () -> ClientSortAndSliceOptions.fromQuery(options));
        assertEquals("unknown is not a sortable field", exception.getMessage());
    }

    @Test
    void descendingSortKeepsNullValuesLast() {
        Comparator<BaseClientRepresentation> comparator = ClientField.DISPLAY_NAME.comparator(false);

        BaseClientRepresentation withDisplayName = client("with-name", "Beta");
        BaseClientRepresentation withoutDisplayName = client("without-name", null);

        assertEquals(1, comparator.compare(withoutDisplayName, withDisplayName));
        assertEquals(-1, comparator.compare(withDisplayName, withoutDisplayName));
    }

    @Test
    void invalidSortDirectionThrowsBadRequest() {
        ListOptions options = new ListOptions();
        options.setSort("clientId|what");

        ServiceException exception = assertThrows(ServiceException.class, () -> ClientSortAndSliceOptions.fromQuery(options));
        assertEquals("sort direction must be asc or desc", exception.getMessage());
    }

    private static BaseClientRepresentation client(String clientId) {
        return client(clientId, clientId);
    }

    private static BaseClientRepresentation client(String clientId, String displayName) {
        BaseClientRepresentation client = new BaseClientRepresentation();
        client.setClientId(clientId);
        client.setDisplayName(displayName);
        return client;
    }
}
