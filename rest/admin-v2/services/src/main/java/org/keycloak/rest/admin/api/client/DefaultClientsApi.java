package org.keycloak.rest.admin.api.client;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.api.ListOptions;
import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.scim.resource.spi.SearchOptions;
import org.keycloak.scim.resource.spi.SortField;
import org.keycloak.scim.resource.spi.SortField.SortOrder;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.scim.ClientResourceTypeProvider;

public class DefaultClientsApi implements ClientsApi {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ClientResourceTypeProvider typeProvider;

    public DefaultClientsApi(@Nonnull KeycloakSession session,
                             @Nonnull RealmModel realm) {
        this.session = session;
        this.realm = realm;
        this.typeProvider = new ClientResourceTypeProvider(session);
    }
    
    /**
     * Parses the raw {@code sort} query parameter into an ordered list of sort fields
     * (field name + direction), without resolving field names against any
     * resource-specific field set.
     */
    static List<SortField> getSortFields(String sort) {
        if (sort == null) {
            return null;
        }
        if (sort.isEmpty()) {
            return List.of();
        }
        List<SortField> parsedSortSegments = Arrays.stream(sort.split(","))
                .map(String::trim)
                .filter(segment -> !segment.isEmpty())
                .map(DefaultClientsApi::parseSortField)
                .collect(Collectors.toUnmodifiableList());
        if (parsedSortSegments.isEmpty()) {
            throw new IllegalArgumentException("sort must specify at least one field");
        }
        return parsedSortSegments;
    }
    
    private static SortField parseSortField(String segment) {
        String[] parts = segment.split("\\|", 2);
        String fieldName = parts[0].trim();
        if (fieldName.isEmpty()) {
            throw new IllegalArgumentException("sort must specify at least one field");
        }
        SortOrder order = parts.length == 1 ? SortOrder.ASC : parseSortOrder(parts[1].trim());
        return new SortField(fieldName, order);
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
        throw new IllegalArgumentException("sort direction must be asc or desc");
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Stream<BaseClientRepresentation> getClients(ListOptions listOptions) {
        try {
            Integer offset = listOptions.getOffset();
            Integer limit = listOptions.getLimit();
            if (offset != null && offset < 0) {
                throw new WebApplicationException("offset must be greater than or equal to 0", Response.Status.BAD_REQUEST);
            }
            if (limit != null && limit < 1) {
                throw new WebApplicationException("limit must be greater than or equal to 1", Response.Status.BAD_REQUEST);
            }
            int normalizedOffset = offset != null ? offset : 0;
            int normalizedLimit = limit != null ? limit : Constants.DEFAULT_MAX_RESULTS;

            var builder = SearchOptions.builder();
            builder.withAttributes(Optional.ofNullable(listOptions.getFields()).map(List::copyOf).orElse(null))
                    .withCount(normalizedLimit).withStartIndex(normalizedOffset).withFilter(listOptions.getQuery());
            
            String sort = listOptions.getSort();
            try {
                if (sort != null && !sort.isEmpty()) {
                    builder.withSort(getSortFields(sort));
                }
            } catch (IllegalArgumentException e) {
                throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
            }
            
            return typeProvider.getAll(builder.build());
            
        } catch (ServiceException e) {
            throw e.toWebApplicationException();
        } catch (org.keycloak.scim.protocol.ForbiddenException e) {
            throw new jakarta.ws.rs.ForbiddenException();
        }
    }

    @POST
    @Override
    public Response createClient(@Valid BaseClientRepresentation client) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(typeProvider.create(client))
                    .build();
        } catch (ServiceException e) {
            throw e.toWebApplicationException();
        } catch (org.keycloak.scim.protocol.ForbiddenException e) {
            throw new jakarta.ws.rs.ForbiddenException();
        }
    }

    /**
     * When the path {@code clientId} does not resolve, return 403 if the caller
     * cannot list clients
     * (anti client-ID phishing), matching {@code ClientsResource#getClient} for
     * Admin API v1.
     */
    private void enforceAntiPhishingIfClientMissing(String clientId) {
        // TODO: align with the scim anti-phishing check once https://github.com/keycloak/keycloak/issues/52147 is addressed
        if (realm.getClientByClientId(clientId) == null && !typeProvider.getPermissions().clients().canList()) {
            throw new ForbiddenException();
        }
    }

    @Path("{id}")
    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        enforceAntiPhishingIfClientMissing(clientId);
        return new DefaultClientApi(session, clientId, typeProvider);
    }

}
